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

package com.io7m.zeptoblog.core;

import com.io7m.jaffirm.core.Preconditions;
import com.io7m.jfunctional.Unit;
import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.rometools.rome.feed.atom.Content;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedOutput;
import javaslang.Tuple2;
import javaslang.collection.Iterator;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.Vector;
import javaslang.control.Option;
import javaslang.control.Validation;
import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.DocType;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static com.io7m.zeptoblog.core.ZBlogPostFormatXHTML.XHTML_URI_TEXT;

/**
 * The default blog writer provider.
 */

@Component(service = ZBlogRendererProviderType.class)
public final class ZBlogRendererProvider implements ZBlogRendererProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogRendererProvider.class);
  }

  private ZBlogPostFormatResolverType resolver;

  /**
   * Construct a blog post renderer provider.
   */

  public ZBlogRendererProvider()
  {
    this.resolver = new ZBlogPostFormatResolverSL();
  }

  /**
   * Set the format resolver.
   *
   * @param in_resolver The post format resolver
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void resolverRegister(
    final ZBlogPostFormatResolverType in_resolver)
  {
    this.resolver = NullCheck.notNull(in_resolver, "in_resolver");
  }

  @Override
  public ZBlogRendererType createRenderer(
    final ZBlogConfiguration config)
  {
    return new Writer(this.resolver, config);
  }

  private static final class Page
  {
    private final Document document;
    private final Element header;
    private final Element content;
    private final Element footer;

    Page(
      final Document in_document,
      final Element in_header,
      final Element in_content,
      final Element in_footer)
    {
      this.document = in_document;
      this.header = in_header;
      this.content = in_content;
      this.footer = in_footer;
    }
  }

  private static final class Writer implements ZBlogRendererType,
    FileVisitor<Path>
  {
    private final ZBlogConfiguration config;
    private final DateTimeFormatter format_date;
    private final DateTimeFormatter format_time;
    private final ZServiceResolverType<ZBlogPostFormatType> resolver;
    private Vector<ZError> errors;
    private Optional<Element> footer_pre;
    private Optional<Element> footer_post;
    private Optional<Element> header_replace;
    private Optional<Element> header_pre;
    private Optional<Element> header_post;

    Writer(
      final ZServiceResolverType<ZBlogPostFormatType> in_resolver,
      final ZBlogConfiguration in_config)
    {
      this.resolver = NullCheck.notNull(in_resolver, "Resolver");
      this.config = NullCheck.notNull(in_config, "config");
      this.errors = Vector.empty();
      this.format_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      this.format_time = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
      this.footer_pre = Optional.empty();
      this.footer_post = Optional.empty();
    }

    private static String version()
    {
      return Writer.class.getPackage().getImplementationVersion();
    }

    private static String ellipsize(
      final String input,
      final int max)
    {
      if (input.length() < max) {
        return input;
      }
      return input.substring(0, max) + "...";
    }

    private static Date dateToTime(final ZonedDateTime time)
    {
      return new Date(TimeUnit.MILLISECONDS.convert(
        time.toEpochSecond(),
        TimeUnit.SECONDS));
    }

    private static Element metaType()
    {
      final Element e = new Element("meta", XHTML_URI_TEXT);
      e.addAttribute(
        new Attribute("http-equiv", null, "Content-Type"));
      e.addAttribute(
        new Attribute("content", null, "application/xhtml+xml; charset=UTF-8"));
      return e;
    }

    private static Element metaGenerator()
    {
      final Element e = new Element("meta", XHTML_URI_TEXT);
      e.addAttribute(
        new Attribute("name", null, "generator"));
      e.addAttribute(
        new Attribute(
          "content",
          null,
          "https://github.com/io7m/zeptoblog; version=" + version()));
      return e;
    }

    private static Element head(
      final String title)
    {
      final Element e = new Element("head", XHTML_URI_TEXT);
      e.appendChild(metaType());
      e.appendChild(metaGenerator());

      final Element e_title = new Element("title", XHTML_URI_TEXT);
      e_title.appendChild(title);
      e.appendChild(e_title);

      {
        final Element e_link = new Element("link", XHTML_URI_TEXT);
        e_link.addAttribute(new Attribute("rel", null, "stylesheet"));
        e_link.addAttribute(new Attribute("type", null, "text/css"));
        e_link.addAttribute(new Attribute("href", null, "/reset.css"));
        e.appendChild(e_link);
      }

      {
        final Element e_link = new Element("link", XHTML_URI_TEXT);
        e_link.addAttribute(new Attribute("rel", null, "stylesheet"));
        e_link.addAttribute(new Attribute("type", null, "text/css"));
        e_link.addAttribute(new Attribute("href", null, "/style.css"));
        e.appendChild(e_link);
      }

      {
        final Element e_link = new Element("link", XHTML_URI_TEXT);
        e_link.addAttribute(new Attribute("rel", null, "alternate"));
        e_link.addAttribute(new Attribute(
          "type",
          null,
          "application/atom+xml"));
        e_link.addAttribute(new Attribute("href", null, "/blog.atom"));
        e.appendChild(e_link);
      }

      return e;
    }

    private static void writeFooter(
      final Page page)
    {
      final Element e = footerPageLinkElement();
      e.appendChild(footerPageLinksByYear());
      page.footer.insertChild(e, 0);
    }

    private static Element footerPageLinks(
      final Tuple2<Integer, Seq<ZBlogPost>> page_current,
      final SortedMap<Integer, Seq<ZBlogPost>> pages)
    {
      final Element e = footerPageLinkElement();
      e.appendChild(footerPageLinksByYear());
      e.appendChild(footerPageLinksByPage(page_current, pages));
      return e;
    }

    private static Element footerPageLinkElement()
    {
      final Element e = new Element("div", XHTML_URI_TEXT);
      e.addAttribute(new Attribute("id", null, "zb_footer_links"));
      return e;
    }

    private static Element footerPageLinksByYear()
    {
      final Element e_yearly = new Element("div", XHTML_URI_TEXT);
      final Element e_a = new Element("a", XHTML_URI_TEXT);
      e_a.addAttribute(new Attribute(
        "href",
        null,
        "/yearly.xhtml"));
      e_a.appendChild("Posts by year");
      e_yearly.appendChild(e_a);
      return e_yearly;
    }

    private static Element footerPageLinksByPage(
      final Tuple2<Integer, Seq<ZBlogPost>> page_current,
      final SortedMap<Integer, Seq<ZBlogPost>> pages)
    {
      final Element e_pages = new Element("div", XHTML_URI_TEXT);
      e_pages.appendChild("Posts by page: ");
      for (final Tuple2<Integer, Seq<ZBlogPost>> pair : pages) {
        final int page_human = pair._1.intValue() + 1;

        if (Objects.equals(page_current._1, pair._1)) {
          e_pages.appendChild(Integer.toString(page_human));
        } else {
          final Element e_a = new Element("a", XHTML_URI_TEXT);
          e_a.addAttribute(new Attribute(
            "href",
            null,
            "/" + page_human + ".xhtml"));
          e_a.appendChild(Integer.toString(page_human));
          e_pages.appendChild(e_a);
        }
        e_pages.appendChild(" ");
      }

      return e_pages;
    }

    private static boolean extensionIsKnown(
      final String extension)
    {
      return Objects.equals(extension, "zbp");
    }

    private Element body(
      final Path current_file)
    {
      final Element e = new Element("body", XHTML_URI_TEXT);
      final Element e_head = new Element("div", XHTML_URI_TEXT);

      {
        final Element e_a = new Element("a", XHTML_URI_TEXT);
        e_a.addAttribute(new Attribute("href", null, "/"));
        e_a.appendChild(this.config.title());

        final Element e_title = new Element("h1", XHTML_URI_TEXT);
        e_title.appendChild(e_a);
        e_head.appendChild(e_title);
      }

      e_head.addAttribute(new Attribute("class", "zb_header"));
      e_head.addAttribute(new Attribute("id", "zb_header"));

      final Element e_content = new Element("div", XHTML_URI_TEXT);
      e_content.addAttribute(new Attribute("class", "zb_body"));
      e_content.addAttribute(new Attribute("id", "zb_body"));

      final Element e_footer = this.footer(current_file);
      e.appendChild(e_head);
      e.appendChild(e_content);
      e.appendChild(e_footer);
      return e;
    }

    private Element footer(
      final Path current_file)
    {
      final Element e_table = new Element("table", XHTML_URI_TEXT);

      {
        final Element e_tr = new Element("tr", XHTML_URI_TEXT);
        e_table.appendChild(e_tr);
        final Element e_td0 = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td0);
        e_td0.appendChild("Signed:");
        final Element e_td1 = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td1);

        final String replaced =
          current_file.toString().replaceAll("\\.xhtml$", ".xhtml.asc");
        final Path absolute =
          current_file.getFileSystem().getPath(replaced).toAbsolutePath();
        final Path relative =
          this.config.outputRoot().toAbsolutePath().relativize(absolute);

        final String sig_name = relative.getFileName().toString();
        final Element e_a = new Element("a", XHTML_URI_TEXT);
        e_a.addAttribute(new Attribute(
          "href",
          null,
          "/" + relative.toString()));
        e_a.appendChild(sig_name);
        e_td1.appendChild(e_a);
      }

      {
        final Element e_tr = new Element("tr", XHTML_URI_TEXT);
        e_table.appendChild(e_tr);
        final Element e_td0 = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td0);
        e_td0.appendChild("Updated:");
        final Element e_td1 = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td1);
        e_td1.appendChild(ZonedDateTime.now().format(this.format_time));
      }

      final Element e_footer = new Element("div", XHTML_URI_TEXT);
      e_footer.addAttribute(new Attribute("class", "zb_footer"));
      e_footer.addAttribute(new Attribute("id", "zb_footer"));
      e_footer.appendChild(e_table);
      return e_footer;
    }

    private Page page(
      final Path current_file,
      final String title)
    {
      final DocType dtype =
        new DocType(
          "html",
          "-//W3C//DTD XHTML 1.0 Strict//EN",
          "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");
      final Element root = new Element("html", XHTML_URI_TEXT);
      root.appendChild(head(title));
      final Element body = this.body(current_file);
      root.appendChild(body);
      final Document doc = new Document(root);
      doc.setDocType(dtype);

      final Element head;
      if (this.header_replace.isPresent()) {
        head = (Element) this.header_replace.get().copy();
        body.replaceChild(body.getChild(0), head);
      } else {
        head = (Element) body.getChild(0);
      }

      this.header_pre.ifPresent(
        element -> head.insertChild(this.header_pre.get().copy(), 0));
      this.header_post.ifPresent(
        element -> head.appendChild(this.header_post.get().copy()));

      final Element foot = (Element) body.getChild(2);
      this.footer_pre.ifPresent(
        element -> foot.insertChild(this.footer_pre.get().copy(), 0));
      this.footer_post.ifPresent(
        element -> foot.appendChild(this.footer_post.get().copy()));

      return new Page(doc, head, (Element) body.getChild(1), foot);
    }

    @Override
    public Validation<Seq<ZError>, Unit> render(
      final ZBlog blog)
    {
      NullCheck.notNull(blog, "Blog");

      this.loadReplacementElements();

      this.generateSegmentPages(blog);
      this.generatePermalinkPages(blog);
      this.generateYearlyPages(blog);
      this.generateAtomFeed(blog);
      this.copyResource("reset.css");
      this.copyResource("style.css");
      this.copyFiles();

      if (this.errors.isEmpty()) {
        return Validation.valid(Unit.unit());
      }

      return Validation.invalid(this.errors);
    }

    private void loadReplacementElements()
    {
      this.footer_pre = this.config.footerPre().flatMap(this::loadXML);
      this.footer_post = this.config.footerPost().flatMap(this::loadXML);
      this.header_pre = this.config.headerPre().flatMap(this::loadXML);
      this.header_post = this.config.headerPost().flatMap(this::loadXML);
      this.header_replace = this.config.headerReplace().flatMap(this::loadXML);
    }

    private Optional<Element> loadXML(
      final Path path)
    {
      try (final InputStream stream = Files.newInputStream(path)) {
        final Builder builder = new Builder();
        return Optional.of(builder.build(stream).getRootElement());
      } catch (final IOException | ParsingException e) {
        this.failException(path, e);
        return Optional.empty();
      }
    }

    private void generateYearlyPages(
      final ZBlog blog)
    {
      final SortedMap<Integer, Seq<ZBlogPost>> posts =
        blog.postsGroupedByYear();

      final Path out_xhtml =
        this.config.outputRoot().resolve("yearly.xhtml").toAbsolutePath();

      final StringBuilder sb = new StringBuilder(128);
      sb.append(this.config.title());
      sb.append(": Posts by year");
      LOG.debug("out: yearly {}", out_xhtml);

      try {
        Files.createDirectories(out_xhtml.getParent());

        try (final OutputStream output = Files.newOutputStream(out_xhtml)) {
          final Page page = this.page(out_xhtml, sb.toString());

          for (final Tuple2<Integer, Seq<ZBlogPost>> pair : posts) {
            page.content.appendChild(
              this.generateYearlyIndex(pair._1, pair._2));
          }

          final Serializer serial = new Serializer(output, "UTF-8");
          serial.write(page.document);
          serial.flush();
        }
      } catch (final IOException e) {
        this.failException(out_xhtml, e);
      }
    }

    private Element generateYearlyIndex(
      final Integer year,
      final Seq<ZBlogPost> posts)
    {
      final Element e = new Element("div", XHTML_URI_TEXT);

      final Element e_title = new Element("h3", XHTML_URI_TEXT);
      e.appendChild(e_title);
      e_title.appendChild(year.toString());

      final Element e_table = new Element("table", XHTML_URI_TEXT);
      e.appendChild(e_table);

      for (final ZBlogPost p : posts) {
        Preconditions.checkPrecondition(
          p.date().isPresent(), "Post must have a date");

        final Element e_tr = new Element("tr", XHTML_URI_TEXT);
        e_table.appendChild(e_tr);

        final Element e_td_date = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td_date);
        e_td_date.appendChild(p.date().get().format(this.format_date));
        e_td_date.addAttribute(new Attribute("class", null, "zb_post_date"));

        final Element e_td_title = new Element("td", XHTML_URI_TEXT);
        e_tr.appendChild(e_td_title);

        final Element e_a = new Element("a", XHTML_URI_TEXT);
        e_a.addAttribute(new Attribute(
          "href",
          null,
          p.outputPermalinkLink(this.config)));
        e_a.appendChild(p.title());

        e_td_title.appendChild(e_a);
      }

      return e;
    }

    private void generateAtomFeed(
      final ZBlog blog)
    {
      final Path out_atom =
        this.config.outputRoot().resolve("blog.atom").toAbsolutePath();

      LOG.debug("atom: {}", out_atom);

      try (final OutputStream output = Files.newOutputStream(out_atom)) {
        final SyndFeed feed = new SyndFeedImpl();
        feed.setFeedType("atom_0.3");
        feed.setTitle(blog.title());
        feed.setDescription("Atom feed");
        feed.setLink(this.config.siteURI().toString());
        feed.setAuthor(this.config.author());

        final SortedMap<ZonedDateTime, ZBlogPost> by_date = blog.postsByDate();
        final Option<Tuple2<ZonedDateTime, ZBlogPost>> last_opt = by_date.lastOption();

        if (last_opt.isDefined()) {
          final Tuple2<ZonedDateTime, ZBlogPost> last = last_opt.get();
          feed.setPublishedDate(dateToTime(last._1));
        }

        final List<SyndEntry> entries =
          new ArrayList<>(by_date.size());
        final Iterator<Tuple2<ZonedDateTime, ZBlogPost>> iterator =
          by_date.map(Function.identity()).reverseIterator();

        while (iterator.hasNext()) {
          final Tuple2<ZonedDateTime, ZBlogPost> entry = iterator.next();
          final ZBlogPost post = entry._2;

          Preconditions.checkPrecondition(
            post.date().isPresent(), "Post must have a date");

          final List<SyndContent> content = new ArrayList<>(1);
          final SyndContentImpl cc = new SyndContentImpl();
          cc.setType(Content.TEXT);
          cc.setValue(ellipsize(post.body().text(), 72));
          cc.setMode(Content.ESCAPED);
          content.add(cc);

          final SyndEntry feed_entry = new SyndEntryImpl();
          final Date date = dateToTime(post.date().get());
          feed_entry.setTitle(post.title());
          feed_entry.setUpdatedDate(date);
          feed_entry.setPublishedDate(date);
          feed_entry.setContents(content);
          feed_entry.setAuthor(this.config.author());
          final String link = post.outputPermalinkLink(this.config);
          LOG.debug("feed link: {}", link);
          feed_entry.setLink(link);
          entries.add(feed_entry);
        }

        feed.setEntries(entries);
        output.write(new SyndFeedOutput().outputString(feed)
                       .getBytes(StandardCharsets.UTF_8));
        output.flush();
      } catch (final IOException | FeedException e) {
        this.failException(out_atom, e);
      }
    }

    private void generateSegmentPages(
      final ZBlog blog)
    {
      final SortedMap<Integer, Seq<ZBlogPost>> pages =
        blog.postsGroupedByPage(this.config.postsPerPage());

      for (final Tuple2<Integer, Seq<ZBlogPost>> pair : pages) {
        final int page_human = pair._1.intValue() + 1;

        final Path out_xhtml =
          this.config.outputRoot().resolve(page_human + ".xhtml").toAbsolutePath();

        final StringBuilder sb = new StringBuilder(128);
        sb.append(this.config.title());
        sb.append(": Page ");
        sb.append(page_human);
        sb.append("/");
        sb.append(pages.size());
        LOG.debug("out: segmented {}", out_xhtml);

        try {
          Files.createDirectories(out_xhtml.getParent());
          try (final OutputStream output = Files.newOutputStream(out_xhtml)) {
            final Page page = this.page(out_xhtml, sb.toString());

            for (final ZBlogPost post : pair._2) {
              page.content.appendChild(this.writePost(post));
            }

            page.footer.insertChild(footerPageLinks(pair, pages), 0);

            final Serializer serial = new Serializer(output, "UTF-8");
            serial.write(page.document);
            serial.flush();
          } catch (final ParsingException e) {
            this.errors = this.errors.append(ZError.of(
              "Could not process post body: " + e.getMessage(),
              LexicalPosition.of(
                0,
                0,
                Optional.of(out_xhtml.toAbsolutePath())),
              Optional.of(e)));
          }
        } catch (final IOException e) {
          this.failException(out_xhtml, e);
        }
      }
    }

    private void generatePermalinkPages(
      final ZBlog blog)
    {
      for (final ZBlogPost post : blog.posts().values()) {
        final Path out_xhtml =
          post.outputPermalinkFileAbsolute(this.config).toAbsolutePath();

        final StringBuilder sb = new StringBuilder(128);
        sb.append(this.config.title());
        sb.append(": ");
        sb.append(post.title());
        LOG.debug("out: permalink {}", out_xhtml);

        try {
          Files.createDirectories(out_xhtml.getParent());
          try (final OutputStream output = Files.newOutputStream(out_xhtml)) {
            final Page page = this.page(out_xhtml, sb.toString());
            page.content.appendChild(this.writePost(post));

            writeFooter(page);

            final Serializer serial = new Serializer(output, "UTF-8");
            serial.write(page.document);
            serial.flush();
          } catch (final ParsingException e) {
            this.errors = this.errors.append(ZError.of(
              "Could not process post body: " + e.getMessage(),
              LexicalPosition.of(
                e.getLineNumber(),
                e.getColumnNumber(),
                Optional.of(out_xhtml.toAbsolutePath())),
              Optional.of(e)));
          }
        } catch (final IOException e) {
          this.failException(out_xhtml, e);
        }
      }
    }

    private void copyFiles()
    {
      try {
        Files.walkFileTree(
          this.config.sourceRoot(),
          EnumSet.noneOf(FileVisitOption.class),
          Integer.MAX_VALUE,
          this);
      } catch (final IOException e) {
        this.failException(this.config.sourceRoot(), e);
      }
    }

    private void copyResource(
      final String name)
    {
      final Path out_path = this.config.outputRoot().resolve(name);
      try {
        LOG.debug("write {} -> {}", name, out_path);

        try (final OutputStream out = Files.newOutputStream(out_path)) {
          final Class<ZBlogRendererProvider> c = ZBlogRendererProvider.class;
          try (final InputStream in =
                 c.getResourceAsStream("/com/io7m/zeptoblog/core/" + name)) {
            IOUtils.copy(in, out);
            out.flush();
          }
        }
      } catch (final IOException e) {
        this.failException(out_path, e);
      }
    }

    private void failException(
      final Path out,
      final Exception e)
    {
      this.errors = this.errors.append(ZError.of(
        e.getMessage(),
        LexicalPosition.of(0, 0, Optional.of(out.toAbsolutePath())),
        Optional.of(e)));
    }

    private Element writePost(
      final ZBlogPost post)
      throws ParsingException, IOException
    {
      final Element e = new Element("div", XHTML_URI_TEXT);
      e.addAttribute(new Attribute("class", "zb_post"));

      final Element e_head = new Element("div", XHTML_URI_TEXT);
      e_head.addAttribute(new Attribute("class", "zb_post_head"));

      final Element e_foot = new Element("div", XHTML_URI_TEXT);
      e_foot.addAttribute(new Attribute("class", "zb_post_foot"));

      post.date().ifPresent(date -> {
        final Element e_date = new Element("span", XHTML_URI_TEXT);
        e_date.addAttribute(new Attribute("class", "zb_post_date"));
        e_date.appendChild(date.format(this.format_date));
        e_head.appendChild(e_date);
        e_head.appendChild(" ");
      });

      {
        final Element e_title = new Element("span", XHTML_URI_TEXT);
        e_title.addAttribute(new Attribute("class", "zb_post_title"));

        final Element e_a = new Element("a", XHTML_URI_TEXT);
        e_a.addAttribute(
          new Attribute("href", null, post.outputPermalinkLink(this.config)));
        e_a.appendChild(post.title());

        e_title.appendChild(e_a);
        e_head.appendChild(e_title);
      }

      final Element e_body = new Element("div", XHTML_URI_TEXT);
      e_body.addAttribute(new Attribute("class", "zb_post_body"));

      {
        final Optional<ZBlogPostFormatType> format_opt =
          this.resolver.resolve(post.body().format());
        if (format_opt.isPresent()) {
          final ZBlogPostFormatType format = format_opt.get();
          final Validation<Seq<ZError>, Element> result =
            format.produceXHTML(post.path(), post.body().text());

          if (result.isValid()) {
            final Element content = result.get();
            for (int index = 0; index < content.getChildCount(); ++index) {
              e_body.appendChild(content.getChild(index).copy());
            }
          } else {
            this.errors = this.errors.appendAll(result.getError());
            throw new IOException("An error occurred in a format provider");
          }
        } else {
          throw new UnsupportedOperationException(
            "No format provider exists for the format: " + post.body().format());
        }
      }

      e.appendChild(e_head);
      e.appendChild(e_body);
      e.appendChild(e_foot);
      return e;
    }

    @Override
    public FileVisitResult preVisitDirectory(
      final Path dir,
      final BasicFileAttributes attrs)
      throws IOException
    {
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFile(
      final Path file,
      final BasicFileAttributes attrs)
      throws IOException
    {
      final String extension = FilenameUtils.getExtension(file.toString());
      if (extension != null && !extensionIsKnown(extension)) {
        final Path relative =
          this.config.sourceRoot().relativize(file.toAbsolutePath());
        final Path output =
          this.config.outputRoot().resolve(relative);

        if (Files.isSymbolicLink(file)) {
          Files.createSymbolicLink(output, Files.readSymbolicLink(file));
        } else {
          LOG.debug("copying {} -> {}", file, output);
          Files.copy(file, output, StandardCopyOption.REPLACE_EXISTING);
        }
      }
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(
      final Path file,
      final IOException exc)
      throws IOException
    {
      this.failException(file, exc);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(
      final Path dir,
      final IOException exc)
      throws IOException
    {
      return FileVisitResult.CONTINUE;
    }
  }
}
