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

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.Seq;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.Node;
import nu.xom.Nodes;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.xslt.XSLException;
import nu.xom.xslt.XSLTransform;
import org.osgi.service.component.annotations.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

/**
 * A format provider for XHTML.
 */

@Component
public final class ZBlogPostFormatXHTML implements ZBlogPostFormatType
{
  /**
   * The name of the format.
   */

  public static final String NAME = "com.io7m.zeptoblog.xhtml";

  /**
   * The XHTML namespace.
   */

  public static final URI XHTML_URI;

  /**
   * The XHTML namespace.
   */

  public static final String XHTML_URI_TEXT;

  static {
    XHTML_URI = URI.create("http://www.w3.org/1999/xhtml");
    XHTML_URI_TEXT = XHTML_URI.toString();
  }

  /**
   * Create a format provider.
   */

  public ZBlogPostFormatXHTML()
  {

  }

  private static void setXHTMLNamespace(
    final Element root)
  {
    root.setNamespaceURI(XHTML_URI_TEXT);
    final Elements elements = root.getChildElements();
    for (int index = 0; index < elements.size(); ++index) {
      final Element element = elements.get(index);
      setXHTMLNamespace(element);
    }
  }

  /**
   * Serialize the given element as a UTF-8 string.
   *
   * @param e The element
   *
   * @return Serialized XML
   */

  public static String serializeXML(
    final Element e)
  {
    try (final ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      final Serializer serial =
        new Serializer(bao, StandardCharsets.UTF_8.name());
      serial.write(new Document(e));
      serial.flush();
      bao.flush();
      return new String(bao.toByteArray(), StandardCharsets.UTF_8);
    } catch (final IOException ex) {
      throw new UnreachableCodeException(ex);
    }
  }

  @Override
  public String name()
  {
    return NAME;
  }

  @Override
  public String description()
  {
    return "XHTML 1.0 Strict";
  }

  @Override
  public Validation<Seq<ZError>, Element> produceXHTML(
    final Path path,
    final String body)
  {
    NullCheck.notNull(path, "Path");
    NullCheck.notNull(body, "Body");

    try (final StringWriter writer = new StringWriter(1024)) {
      writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      writer.append(System.lineSeparator());
      writer.append("<div xmlns=\"");
      writer.append(XHTML_URI_TEXT);
      writer.append("\"");
      writer.append(System.lineSeparator());
      writer.append(body);
      writer.append(System.lineSeparator());
      writer.append("</div>");
      writer.append(System.lineSeparator());
      writer.flush();

      try (final StringReader reader = new StringReader(body)) {
        final Builder b = new Builder();
        final Document doc = b.build(reader, path.toUri().toString());
        final Element root = doc.getRootElement();
        setXHTMLNamespace(root);
        return valid((Element) root.copy());
      } catch (final ParsingException e) {
        return invalid(Vector.of(
          ZError.of(
            e.getMessage(),
            LexicalPosition.of(
              e.getLineNumber(),
              e.getColumnNumber(),
              Optional.of(path)),
            Optional.of(e))));
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
    NullCheck.notNull(path, "Path");
    NullCheck.notNull(text, "Text");

    return this.produceXHTML(path, text).flatMap(e -> plain(path, e));
  }

  private static Validation<Seq<ZError>, String> plain(
    final Path path,
    final Element e)
  {
    try (final InputStream is =
           ZBlogPostFormatXHTML.class.getResourceAsStream("plain.xsl")) {
      try (final InputStreamReader r =
             new InputStreamReader(is, StandardCharsets.UTF_8)) {
        final Builder b = new Builder();
        final Document stylesheet = b.build(r, path.toUri().toString());
        final XSLTransform transform = new XSLTransform(stylesheet);
        final Document doc = new Document(e);
        final Nodes nodes = transform.transform(doc);

        final StringBuilder text = new StringBuilder(256);
        for (int index = 0; index < nodes.size(); ++index) {
          final Node node = nodes.get(index);
          text.append(node.getValue());
          text.append(System.lineSeparator());
        }

        final String[] lines = text.toString().split("\\r?\\n");
        text.setLength(0);
        for (int index = 0; index < lines.length; ++index) {
          text.append(lines[index].trim());
          text.append(System.lineSeparator());
        }

        return valid(text.toString());
      }

    } catch (final XSLException | IOException ex) {
      return invalid(Vector.of(
        ZError.of(
          ex.getMessage(),
          LexicalPosition.of(0, 0, Optional.of(path)),
          Optional.of(ex))));
    } catch (final ParsingException ex) {
      return invalid(Vector.of(
        ZError.of(
          ex.getMessage(),
          LexicalPosition.of(
            ex.getLineNumber(), ex.getColumnNumber(), Optional.of(path)),
          Optional.of(ex))));
    }
  }
}
