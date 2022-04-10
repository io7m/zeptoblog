/*
 * Copyright © 2017 Mark Raynsford <code@io7m.com> https://www.io7m.com
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

package com.io7m.zeptoblog.commonmark;

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import com.io7m.zeptoblog.core.ZBlogPostFormatType;
import com.io7m.zeptoblog.core.ZError;
import com.io7m.zeptoblog.core.ZXML;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;
import org.commonmark.Extension;
import org.commonmark.ext.heading.anchor.HeadingAnchorExtension;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.NodeRenderer;
import org.commonmark.renderer.html.HtmlNodeRendererContext;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.html.HtmlWriter;
import org.commonmark.renderer.text.TextContentNodeRendererContext;
import org.commonmark.renderer.text.TextContentRenderer;
import org.commonmark.renderer.text.TextContentWriter;
import org.osgi.service.component.annotations.Component;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * A format provider for the <i>CommonMark</i> format.
 *
 * @see <a href="http://commonmark.org">http://commonmark.org</a>
 */

@Component
public final class ZBlogPostFormatCommonMark implements ZBlogPostFormatType
{
  /**
   * The name of the format.
   */

  public static final String NAME = "com.io7m.zeptoblog.commonmark";

  /**
   * Create a format provider.
   */

  public ZBlogPostFormatCommonMark()
  {

  }

  @Override
  public String description()
  {
    return "http://commonmark.org 0.27";
  }

  @Override
  public String name()
  {
    return NAME;
  }

  @Override
  public Validation<Seq<ZError>, Element> produceXHTML(
    final Path path,
    final String body)
  {
    final Parser parser = Parser.builder().build();
    final Node document = parser.parse(body);
    final List<Extension> extensions =
      Collections.singletonList(HeadingAnchorExtension.create());

    final HtmlRenderer renderer =
      HtmlRenderer.builder()
        .nodeRendererFactory(IndentedCodeBlockNodeRenderer::new)
        .extensions(extensions)
        .build();

    try (StringWriter writer = new StringWriter(1024)) {
      writer.append("<div xmlns=\"http://www.w3.org/1999/xhtml\">");
      final String separator = System.lineSeparator();
      writer.append(separator);
      renderer.render(document, writer);
      writer.append("</div>");
      writer.append(separator);
      writer.flush();

      try (InputStream stream =
             new ByteArrayInputStream(writer.toString().getBytes(UTF_8))) {
        return valid(ZXML.xmlParseFromStream(
          path,
          stream).getDocumentElement());
      } catch (final SAXException | ParserConfigurationException ex) {
        return invalid(Vector.of(
          ZError.of(
            ex.getMessage(),
            LexicalPosition.of(0, 0, Optional.of(path)),
            Optional.of(ex))));
      }
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  @Override
  public Validation<Seq<ZError>, String> producePlain(
    final Path path,
    final String text)
  {
    final Parser parser = Parser.builder().build();
    final Node document = parser.parse(text);

    final TextContentRenderer renderer =
      TextContentRenderer.builder()
        .nodeRendererFactory(TextRenderer::new)
        .build();

    try (StringWriter writer = new StringWriter(1024)) {
      renderer.render(document, writer);
      writer.append(System.lineSeparator());
      writer.flush();

      return valid(writer.toString());
    } catch (final IOException e) {
      throw new UnreachableCodeException(e);
    }
  }

  private static final class TextRenderer
    implements NodeRenderer
  {
    private final TextContentWriter writer;
    private final TextContentNodeRendererContext context;

    TextRenderer(
      final TextContentNodeRendererContext in_context)
    {
      this.context =
        Objects.requireNonNull(in_context, "Context");
      this.writer =
        Objects.requireNonNull(in_context.getWriter(), "Writer");
    }

    @Override
    public Set<Class<? extends Node>> getNodeTypes()
    {
      final Set<Class<? extends Node>> s = new HashSet<>(1);
      s.add(Paragraph.class);
      return s;
    }

    @Override
    public void render(final Node node)
    {
      if (node instanceof Paragraph) {
        Node ch = node.getFirstChild();
        while (ch != null) {
          this.context.render(ch);
          ch = ch.getNext();
        }
        this.writer.line();
        this.writer.write(System.lineSeparator());
      }
    }
  }

  private static final class IndentedCodeBlockNodeRenderer
    implements NodeRenderer
  {
    private final HtmlWriter html;

    IndentedCodeBlockNodeRenderer(
      final HtmlNodeRendererContext context)
    {
      this.html = Objects.requireNonNull(context, "Context").getWriter();
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
