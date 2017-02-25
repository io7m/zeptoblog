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

package com.io7m.zeptoblog;

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
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * The default blog writer provider.
 */

@Component(service = ZBlogWriterProviderType.class)
public final class ZBlogWriterProvider implements ZBlogWriterProviderType
{
  private static final Logger LOG;
  private static final URI XHTML_URI;
  private static final String XHTML_URI_TEXT;

  static {
    LOG = LoggerFactory.getLogger(ZBlogWriterProvider.class);
    XHTML_URI = URI.create("http://www.w3.org/1999/xhtml");
    XHTML_URI_TEXT = XHTML_URI.toString();
  }

  /**
   * Construct a new blog writer provider.
   */

  public ZBlogWriterProvider()
  {

  }

  @Override
  public ZBlogWriterType createWriter(
    final ZBlogConfiguration config)
  {
    return new Writer(config);
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

  private static final class Writer implements ZBlogWriterType,
    FileVisitor<Path>
  {
    private final ZBlogConfiguration config;
    private final DateTimeFormatter format_date;
    private final DateTimeFormatter format_time;
    private Vector<ZError> errors;

    Writer(
      final ZBlogConfiguration in_config)
    {
      this.config = NullCheck.notNull(in_config, "config");
      this.errors = Vector.empty();
      this.format_date = DateTimeFormatter.ofPattern("yyyy-MM-dd");
      this.format_time = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    private Element meta()
    {
      final Element e = new Element("meta", XHTML_URI_TEXT);
      e.addAttribute(
        new Attribute("http-equiv", null, "Content-Type"));
      e.addAttribute(
        new Attribute("content", null, "application/xhtml+xml; charset=UTF-8"));
      return e;
    }

    private Element head(
      final String title)
    {
      final Element e = new Element("head", XHTML_URI_TEXT);
      e.appendChild(this.meta());

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

    private Element body(
      final Path current_file)
    {
      final Element e = new Element("body", XHTML_URI_TEXT);
      final Element e_head = new Element("div", XHTML_URI_TEXT);

      final Element e_title = new Element("h2", XHTML_URI_TEXT);
      e_title.appendChild(this.config.title());
      e_head.appendChild(e_title);
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
      root.appendChild(this.head(title));
      final Element body = this.body(current_file);
      root.appendChild(body);
      final Document doc = new Document(root);
      doc.setDocType(dtype);

      return new Page(
        doc,
        (Element) body.getChild(0),
        (Element) body.getChild(1),
        (Element) body.getChild(2));
    }

    @Override
    public Validation<Seq<ZError>, Unit> write(
      final ZBlog blog)
    {
      NullCheck.notNull(blog, "Blog");

      this.generateSegmentPages(blog);
      this.generatePermalinkPages(blog);
      this.generateAtomFeed(blog);
      this.copyResource("reset.css");
      this.copyResource("style.css");
      this.copyFiles();

      if (this.errors.isEmpty()) {
        return Validation.valid(Unit.unit());
      }

      return Validation.invalid(this.errors);
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

          final List<SyndContent> content = new ArrayList<>(1);
          final SyndContentImpl cc = new SyndContentImpl();
          cc.setType(Content.TEXT);
          cc.setValue(ellipsize(post.body(), 72));
          cc.setMode(Content.ESCAPED);
          content.add(cc);

          final SyndEntry feed_entry = new SyndEntryImpl();
          final Date date = dateToTime(post.date());
          feed_entry.setTitle(post.title());
          feed_entry.setUpdatedDate(date);
          feed_entry.setPublishedDate(date);
          feed_entry.setContents(content);
          feed_entry.setAuthor(this.config.author());
          feed_entry.setLink(post.outputPermalinkLink(this.config));
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

    private static Date dateToTime(final ZonedDateTime time)
    {
      return new Date(TimeUnit.MILLISECONDS.convert(
        time.toEpochSecond(),
        TimeUnit.SECONDS));
    }

    private void generateSegmentPages(
      final ZBlog blog)
    {
      final SortedMap<Integer, Seq<ZBlogPost>> pages =
        blog.postsByPage(this.config.postsPerPage());

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

            page.footer.insertChild(this.footerPageLinks(pair, pages), 0);

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
      final SortedMap<ZonedDateTime, ZBlogPost> by_date = blog.postsByDate();

      final Iterator<Tuple2<ZonedDateTime, ZBlogPost>> iterator =
        by_date.map(Function.identity()).reverseIterator();

      while (iterator.hasNext()) {
        final Tuple2<ZonedDateTime, ZBlogPost> pair = iterator.next();

        final ZBlogPost post = pair._2;
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

    private Element footerPageLinks(
      final Tuple2<Integer, Seq<ZBlogPost>> page_current,
      final SortedMap<Integer, Seq<ZBlogPost>> pages)
    {
      final Element e = new Element("div", XHTML_URI_TEXT);
      e.addAttribute(new Attribute("id", null, "zb_footer_links"));

      for (final Tuple2<Integer, Seq<ZBlogPost>> pair : pages) {
        final int page_human = pair._1.intValue() + 1;

        if (Objects.equals(page_current._1, pair._1)) {
          e.appendChild(Integer.toString(page_human));
        } else {
          final Element e_a = new Element("a", XHTML_URI_TEXT);
          e_a.addAttribute(new Attribute(
            "href",
            null,
            "/" + page_human + ".xhtml"));
          e_a.appendChild(Integer.toString(page_human));
          e.appendChild(e_a);
        }
        e.appendChild(" ");
      }

      return e;
    }

    private void copyResource(
      final String name)
    {
      final Path out_path = this.config.outputRoot().resolve(name);
      try {
        LOG.debug("write {} -> {}", name, out_path);

        try (final OutputStream out = Files.newOutputStream(out_path)) {
          final Class<ZBlogWriterProvider> c = ZBlogWriterProvider.class;
          try (final InputStream in =
                 c.getResourceAsStream("/com/io7m/zeptoblog/" + name)) {
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
      e.addAttribute(new Attribute(
        "id",
        null,
        "zb_post_" + post.idShort(this.config)));
      e.addAttribute(new Attribute("class", "zb_post"));

      final Element e_head = new Element("div", XHTML_URI_TEXT);
      e_head.addAttribute(new Attribute("class", "zb_post_head"));

      final Element e_permalink = new Element("a", XHTML_URI_TEXT);
      e_permalink.appendChild("Permalink");
      e_permalink.addAttribute(
        new Attribute("href", null, post.outputPermalinkLink(this.config)));

      final Element e_foot = new Element("div", XHTML_URI_TEXT);
      e_foot.addAttribute(new Attribute("class", "zb_post_foot"));
      e_foot.appendChild(e_permalink);

      final Element e_date = new Element("span", XHTML_URI_TEXT);
      e_date.addAttribute(new Attribute("class", "zb_post_date"));
      e_date.appendChild(post.date().format(this.format_date));
      e_head.appendChild(e_date);

      {
        final Element e_title = new Element("span", XHTML_URI_TEXT);
        e_title.addAttribute(new Attribute("class", "zb_post_title"));

        final Element e_a = new Element("a", XHTML_URI_TEXT);
        e_a.addAttribute(
          new Attribute("href", null, "#zb_post_" + post.idShort(this.config)));
        e_a.appendChild(post.title());

        e_title.appendChild(e_a);
        e_head.appendChild(e_title);
      }

      final Element e_body = new Element("div", XHTML_URI_TEXT);
      e_body.addAttribute(new Attribute("class", "zb_post_body"));

      final Element content = this.processPostBody(post.body());
      for (int index = 0; index < content.getChildCount(); ++index) {
        e_body.appendChild(content.getChild(index).copy());
      }

      e.appendChild(e_head);
      e.appendChild(e_body);
      e.appendChild(e_foot);
      return e;
    }

    private Element processPostBody(
      final String body)
      throws ParsingException, IOException
    {
      final Parser parser = Parser.builder().build();
      final org.commonmark.node.Node document = parser.parse(body);
      final HtmlRenderer renderer =
        HtmlRenderer.builder()
          .nodeRendererFactory(IndentedCodeBlockNodeRenderer::new)
          .build();

      try (final StringWriter writer = new StringWriter(1024)) {
        writer.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
        writer.append(System.lineSeparator());
        renderer.render(document, writer);
        writer.append("</div>");
        writer.append(System.lineSeparator());
        writer.flush();

        try (final StringReader reader = new StringReader(writer.toString())) {
          final Builder b = new Builder();
          final Document doc = b.build(reader);
          final Element root = doc.getRootElement();
          return (Element) root.copy();
        }
      }
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
      if (extension != null && !Objects.equals(extension, "zbp")) {
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

  private static final class IndentedCodeBlockNodeRenderer
    implements NodeRenderer
  {
    private final HtmlWriter html;

    IndentedCodeBlockNodeRenderer(
      final HtmlNodeRendererContext context)
    {
      this.html = context.getWriter();
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes()
    {
      final Set<Class<? extends Node>> s = new HashSet<>(2);
      s.add(IndentedCodeBlock.class);
      s.add(FencedCodeBlock.class);
      return s;
    }

    @Override
    public void render(final Node node)
    {
      if (node instanceof IndentedCodeBlock) {
        this.html.line();
        this.html.tag("pre");
        this.html.text(((IndentedCodeBlock) node).getLiteral());
        this.html.tag("/pre");
        this.html.line();
      }

      if (node instanceof FencedCodeBlock) {
        this.html.line();
        this.html.tag("pre");
        this.html.text(((FencedCodeBlock) node).getLiteral());
        this.html.tag("/pre");
        this.html.line();
      }
    }
  }
}
