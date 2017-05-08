/*
 * Copyright © 2017 <code@io7m.com> http://io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package com.io7m.zeptoblog.glossary;

import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogPost;
import com.io7m.zeptoblog.core.ZBlogPostBody;
import com.io7m.zeptoblog.core.ZBlogPostFormatResolverSL;
import com.io7m.zeptoblog.core.ZBlogPostFormatResolverType;
import com.io7m.zeptoblog.core.ZBlogPostFormatType;
import com.io7m.zeptoblog.core.ZBlogPostFormatXHTML;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorType;
import com.io7m.zeptoblog.core.ZError;
import javaslang.collection.List;
import javaslang.collection.Seq;
import javaslang.collection.Set;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import nu.xom.Attribute;
import nu.xom.Element;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import static com.io7m.zeptoblog.core.ZBlogPostFormatXHTML.XHTML_URI_TEXT;
import static com.io7m.zeptoblog.core.ZErrors.ofException;
import static com.io7m.zeptoblog.core.ZErrors.ofMessagePath;
import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.sequence;
import static javaslang.control.Validation.valid;

/**
 * A glossary generator.
 */

@Component(service = ZBlogPostGeneratorType.class)
public final class ZGlossaryGenerator implements ZBlogPostGeneratorType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZGlossaryGenerator.class);
  }

  private volatile ZGlossaryParserProviderType parsers;
  private volatile ZBlogPostFormatResolverType formats;

  /**
   * Construct a glossary generator.
   */

  public ZGlossaryGenerator()
  {
    this.parsers = new ZGlossaryParserProvider();
    this.formats = new ZBlogPostFormatResolverSL();
  }

  /**
   * Introduce a glossary parser provider.
   *
   * @param in_provider The parser provider
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void setGlossaryParserProvider(
    final ZGlossaryParserProviderType in_provider)
  {
    this.parsers = NullCheck.notNull(in_provider, "provider");
  }

  /**
   * Introduce a post formats.
   *
   * @param in_formats The post formats
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void setPostFormatResolver(
    final ZBlogPostFormatResolverType in_formats)
  {
    this.formats = NullCheck.notNull(in_formats, "formats");
  }

  @Override
  public String name()
  {
    return "com.io7m.zeptoblog.glossary";
  }

  @Override
  public String description()
  {
    return "A glossary generator";
  }

  @Override
  public Validation<Seq<ZError>, SortedMap<Path, ZBlogPost>> generate(
    final ZBlogConfiguration config,
    final Properties props)
  {
    return new Generator(this.parsers, this.formats, config, props).run();
  }

  private static final class Generator
  {
    private final ZBlogConfiguration config;
    private final Properties props;
    private final ZGlossaryParserProviderType parsers;
    private final ZBlogPostFormatResolverType formats;

    Generator(
      final ZGlossaryParserProviderType in_parsers,
      final ZBlogPostFormatResolverType in_formats,
      final ZBlogConfiguration in_config,
      final Properties in_props)
    {
      this.parsers = NullCheck.notNull(in_parsers, "Parsers");
      this.formats = NullCheck.notNull(in_formats, "Formats");
      this.config = NullCheck.notNull(in_config, "Config");
      this.props = NullCheck.notNull(in_props, "Props");
    }

    private static Validation<Seq<ZError>, SortedMap<Path, ZBlogPost>>
    generate(
      final ZBlogPostFormatResolverType formats,
      final Path output_file,
      final ZGlossary glossary)
    {
      return createGlossaryPost(formats, output_file, glossary)
        .<SortedMap<Path, ZBlogPost>>flatMap(post -> valid(TreeMap.of(
          output_file,
          post)))
        .mapError(Vector::ofAll);
    }

    private static Validation<List<ZError>, ZBlogPost>
    createGlossaryPost(
      final ZBlogPostFormatResolverType formats,
      final Path output_file,
      final ZGlossary glossary)
    {
      return transformGlossary(formats, glossary)
        .flatMap(e -> valid(ZBlogPost.of(
          "Glossary", Optional.empty(), output_file, bodyOfElement(e))));
    }

    private static ZBlogPostBody bodyOfElement(
      final Element e)
    {
      return ZBlogPostBody.of(
        ZBlogPostFormatXHTML.NAME,
        ZBlogPostFormatXHTML.serializeXML(e));
    }

    private static Validation<List<ZError>, Element>
    transformGlossary(
      final ZBlogPostFormatResolverType formats,
      final ZGlossary glossary)
    {
      return sequence(
        glossary.itemsByLetter()
          .toList()
          .map(p -> transformLetter(formats, p._1, p._2)))
        .flatMap(elements -> {
          final Element e_container = new Element("div", XHTML_URI_TEXT);
          elements.forEach(e_container::appendChild);
          return valid(e_container);
        });
    }

    private static Validation<List<ZError>, Element>
    transformItemBody(
      final ZBlogPostFormatResolverType formats,
      final ZGlossaryItem item)
    {
      LOG.trace("transformItemBody: {}", item.term());

      final ZGlossaryItemBody body = item.body();
      final String format_name = body.format();
      final Optional<ZBlogPostFormatType> format_opt =
        formats.resolve(format_name);

      if (format_opt.isPresent()) {
        final ZBlogPostFormatType format = format_opt.get();
        return format.produceXHTML(item.path(), body.text())
          .mapError(List::ofAll);
      }

      return invalid(List.of(ofMessagePath(
        "No provider for format: " + format_name, item.path())));
    }

    private static Validation<List<ZError>, Element>
    transformItemEnclose(
      final ZGlossaryItem item,
      final Element e_body)
    {
      LOG.trace("transformItemEnclose: {}", item.term());

      final Element e_container = new Element("div", XHTML_URI_TEXT);
      e_container.addAttribute(
        new Attribute("class", "zb_glossary_item"));
      e_container.appendChild(
        title("h3", item.targetID(), item.term()));
      e_container.appendChild(e_body);

      final Set<String> related = item.seeAlso();
      if (!related.isEmpty()) {
        final Element e_related = new Element("div", XHTML_URI_TEXT);
        e_related.addAttribute(
          new Attribute("class", "zb_glossary_related"));
        e_related.appendChild("See: ");

        related.forEach(term -> {
          final String r_id = item.targetID();
          final Element e_link = new Element("a", XHTML_URI_TEXT);
          e_link.addAttribute(new Attribute("href", null, "#" + r_id));
          e_link.appendChild(term);
          e_related.appendChild(e_link);
          e_related.appendChild(" ");
        });

        e_container.appendChild(e_related);
      }

      return valid(e_container);
    }

    private static Validation<List<ZError>, Element>
    transformItem(
      final ZBlogPostFormatResolverType formats,
      final ZGlossaryItem item)
    {
      LOG.trace("transformItem: {}", item.term());
      return transformItemBody(formats, item)
        .flatMap(body -> transformItemEnclose(item, body));
    }

    private static Validation<List<ZError>, Element>
    transformLetter(
      final ZBlogPostFormatResolverType formats,
      final String letter,
      final SortedMap<String, ZGlossaryItem> terms)
    {
      LOG.trace(
        "transformLetter: {} ({} terms)",
        letter,
        Integer.valueOf(terms.size()));

      return sequence(terms.values().map(item -> transformItem(formats, item)))
        .flatMap(elements -> {
          final Element e_container = new Element("div", XHTML_URI_TEXT);
          e_container.addAttribute(new Attribute(
            "class",
            "zb_glossary_letter"));

          e_container.appendChild(title("h2", letter.toLowerCase(), letter));
          elements.forEach(e_container::appendChild);
          e_container.appendChild(new Element("hr", XHTML_URI_TEXT));
          return valid(e_container);
        });
    }

    private static Element title(
      final String type,
      final String id,
      final String text)
    {
      final Element e_title_h2 = new Element(type, XHTML_URI_TEXT);
      final Element e_title = new Element("a", XHTML_URI_TEXT);
      e_title.addAttribute(
        new Attribute("href", null, "#" + id));
      e_title.addAttribute(
        new Attribute("id", null, id));
      e_title.appendChild(text);
      e_title_h2.appendChild(e_title);
      return e_title_h2;
    }

    private static Validation<Seq<ZError>, ZGlossary> runParse(
      final ZBlogConfiguration in_config,
      final ZGlossaryParserProviderType in_parsers,
      final Path in_path)
    {
      LOG.debug("parsing {}", in_path);
      return in_parsers.createParser(in_config, in_path).parse();
    }

    private static Validation<Seq<ZError>, Path> getSourcePath(
      final ZBlogConfiguration config,
      final Properties in_props)
    {
      try {
        return valid(
          config.sourceRoot().getFileSystem().getPath(
            JProperties.getString(
              in_props, "com.io7m.zeptoblog.glossary.source_dir")));
      } catch (final JPropertyNonexistent ex) {
        return invalid(Vector.of(ofException(ex)));
      }
    }

    private static Validation<Seq<ZError>, Path> getOutputPath(
      final ZBlogConfiguration config,
      final Properties in_props)
    {
      try {
        return valid(
          config.sourceRoot().getFileSystem().getPath(
            JProperties.getString(
              in_props, "com.io7m.zeptoblog.glossary.output_file")));
      } catch (final JPropertyNonexistent ex) {
        return invalid(Vector.of(ofException(ex)));
      }
    }

    public Validation<Seq<ZError>, SortedMap<Path, ZBlogPost>> run()
    {
      final ZBlogConfiguration c = this.config;
      final Properties p = this.props;
      final ZGlossaryParserProviderType ps = this.parsers;
      final ZBlogPostFormatResolverType fs = this.formats;

      return getSourcePath(c, p)
        .flatMap(source_path -> getOutputPath(c, p)
          .flatMap(output_path -> runParse(c, ps, source_path)
            .flatMap(glossary -> generate(fs, output_path, glossary))));
    }
  }
}
