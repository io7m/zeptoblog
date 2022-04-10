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

package com.io7m.zeptoblog.core;

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.junreachable.UnreachableCodeException;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;
import org.osgi.service.component.annotations.Component;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;
import static java.nio.charset.StandardCharsets.UTF_8;

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
    try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
      ZXML.xmlSerializeElementToStream(bao, e);
      return bao.toString(UTF_8.name());
    } catch (final IOException | TransformerException | ParserConfigurationException ex) {
      throw new UnreachableCodeException(ex);
    }
  }

  private static Validation<Seq<ZError>, String> plain(
    final Path path,
    final Element e)
  {
    try (InputStream is =
           ZBlogPostFormatXHTML.class.getResourceAsStream("plain.xsl")) {
      try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {

        ZXML.xmlTransformElementStream(output, e, is);
        final String raw_text = output.toString(UTF_8.name());
        final String[] lines = raw_text.split("\\r?\\n");
        final StringBuilder text = new StringBuilder(256);
        text.setLength(0);
        for (int index = 0; index < lines.length; ++index) {
          text.append(lines[index].trim());
          text.append(System.lineSeparator());
        }

        return valid(text.toString());
      }
    } catch (final IOException | ParserConfigurationException | TransformerException ex) {
      return invalid(Vector.of(
        ZError.of(
          ex.getMessage(),
          LexicalPosition.of(0, 0, Optional.of(path)),
          Optional.of(ex))));
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
    Objects.requireNonNull(path, "Path");
    Objects.requireNonNull(body, "Body");

    final String separator = System.lineSeparator();
    try (StringWriter writer = new StringWriter(1024)) {
      writer.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      writer.append(separator);
      writer.append("<div xmlns=\"");
      writer.append(XHTML_URI_TEXT);
      writer.append("\">");
      writer.append(separator);
      writer.append(body);
      writer.append(separator);
      writer.append("</div>");
      writer.append(separator);
      writer.flush();

      try (InputStream stream =
             new ByteArrayInputStream(writer.toString().getBytes(UTF_8))) {
        return valid(ZXML.xmlParseFromStream(path, stream)
                       .getDocumentElement());
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
    Objects.requireNonNull(path, "Path");
    Objects.requireNonNull(text, "Text");

    return this.produceXHTML(path, text).flatMap(e -> plain(path, e));
  }
}
