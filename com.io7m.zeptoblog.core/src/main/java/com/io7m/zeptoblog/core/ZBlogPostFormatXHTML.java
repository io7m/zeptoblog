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
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.Seq;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Elements;
import nu.xom.ParsingException;
import org.osgi.service.component.annotations.Component;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * A format provider for XHTML.
 */

@Component
public final class ZBlogPostFormatXHTML implements
  ZBlogPostFormatType
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
        final Document doc = b.build(reader);
        final Element root = doc.getRootElement();
        setXHTMLNamespace(root);
        return Validation.valid((Element) root.copy());
      } catch (final ParsingException e) {
        return Validation.invalid(Vector.of(
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
}
