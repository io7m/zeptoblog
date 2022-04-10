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

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * XML utility functions.
 */

public final class ZXML
{
  private ZXML()
  {

  }

  /**
   * Transform the given document using the given stylesheet, writing the result
   * to the given output stream.
   *
   * @param stream     The output stream
   * @param document   The input document
   * @param stylesheet The stylesheet
   *
   * @throws TransformerException On transform errors
   */

  public static void xmlTransformStream(
    final OutputStream stream,
    final Document document,
    final InputStream stylesheet)
    throws TransformerException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(document, "document");

    final TransformerFactory transformer_factory =
      TransformerFactory.newInstance();
    final Transformer transformer =
      transformer_factory.newTransformer(new StreamSource(stylesheet));

    transformer.transform(new DOMSource(document), new StreamResult(stream));
  }

  /**
   * Transform the given element using the given stylesheet, writing the result
   * to the given output stream.
   *
   * @param stream     The output stream
   * @param element    The input element
   * @param stylesheet The stylesheet
   *
   * @throws TransformerException         On transform errors
   * @throws ParserConfigurationException On parser configuration errors
   */

  public static void xmlTransformElementStream(
    final OutputStream stream,
    final Element element,
    final InputStream stylesheet)
    throws TransformerException, ParserConfigurationException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(element, "element");
    Objects.requireNonNull(stylesheet, "stylesheet");

    final Document document = xmlNewDocument();
    document.appendChild(document.importNode(element, true));
    xmlTransformStream(stream, document, stylesheet);
  }

  /**
   * Serialize the given document to the output stream.
   *
   * @param stream   The output stream
   * @param document The input document
   *
   * @throws TransformerException On transform errors
   */

  public static void xmlSerializeToStream(
    final OutputStream stream,
    final Document document)
    throws TransformerException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(document, "document");

    final TransformerFactory transformer_factory =
      TransformerFactory.newInstance();
    transformer_factory.setAttribute("indent-number", Integer.valueOf(2));

    final Transformer transformer =
      transformer_factory.newTransformer();

    xmlSerialize(stream, document, transformer);
  }

  /**
   * Serialize the given document to the output stream, emitting an XHTML
   * doctype if requested.
   *
   * @param stream   The output stream
   * @param document The input document
   * @param doctype  {@code true} if the output should contain a DOCTYPE
   *
   * @throws TransformerException On transform errors
   */

  public static void xhtmlSerializeToStream(
    final OutputStream stream,
    final Document document,
    final boolean doctype)
    throws TransformerException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(document, "document");

    final TransformerFactory transformer_factory =
      TransformerFactory.newInstance();
    transformer_factory.setAttribute("indent-number", Integer.valueOf(2));

    final Transformer transformer =
      transformer_factory.newTransformer();

    if (doctype) {
      xhtmlSerialize(stream, document, transformer);
    } else {
      xmlSerialize(stream, document, transformer);
    }
  }

  private static void xhtmlSerialize(
    final OutputStream stream,
    final Document document,
    final Transformer transformer)
    throws TransformerException
  {
    final DOMImplementation dom = document.getImplementation();
    final DocumentType doctype =
      dom.createDocumentType(
        "html",
        "-//W3C//DTD XHTML 1.0 Strict//EN",
        "http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd");

    transformer.setOutputProperty(
      OutputKeys.DOCTYPE_PUBLIC, doctype.getPublicId());
    transformer.setOutputProperty(
      OutputKeys.DOCTYPE_SYSTEM, doctype.getSystemId());

    xmlSerialize(stream, document, transformer);
  }

  private static void xmlSerialize(
    final OutputStream stream,
    final Document document,
    final Transformer transformer)
    throws TransformerException
  {
    transformer.setOutputProperty(
      OutputKeys.INDENT, "no");
    transformer.setOutputProperty(
      "{http://xml.apache.org/xslt}indent-amount", "0");
    transformer.setOutputProperty(
      OutputKeys.OMIT_XML_DECLARATION, "yes");

    transformer.transform(
      new DOMSource(document),
      new StreamResult(new OutputStreamWriter(stream, StandardCharsets.UTF_8)));
  }

  /**
   * Serialize the given element to the output stream.
   *
   * @param stream  The output stream
   * @param element The input element
   *
   * @throws TransformerException         On transform errors
   * @throws ParserConfigurationException On parser configuration errors
   */

  public static void xmlSerializeElementToStream(
    final OutputStream stream,
    final Element element)
    throws TransformerException, ParserConfigurationException
  {
    Objects.requireNonNull(stream, "stream");
    Objects.requireNonNull(element, "element");

    final Document document = xmlNewDocument();
    document.appendChild(document.importNode(element, true));
    xmlSerializeToStream(stream, document);
  }

  /**
   * Create a new empty document.
   *
   * @return A new document
   *
   * @throws ParserConfigurationException On parser configuration errors
   */

  public static Document xmlNewDocument()
    throws ParserConfigurationException
  {
    final DocumentBuilderFactory factory = DocumentBuilderFactory.newDefaultInstance();
    final DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.newDocument();
  }

  /**
   * Parse an XML document from the given path.
   *
   * @param path The input path
   *
   * @return The parsed document
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws IOException                  On I/O errors
   * @throws SAXException                 On parse errors
   */

  public static Document xmlParseFromPath(final Path path)
    throws ParserConfigurationException, IOException, SAXException
  {
    Objects.requireNonNull(path, "path");

    try (InputStream stream = Files.newInputStream(path)) {
      return xmlParseFromStream(path, stream);
    }
  }

  /**
   * Parse an XML document from the given path and input stream.
   *
   * @param path   The input path
   * @param stream The input stream
   *
   * @return The parsed document
   *
   * @throws ParserConfigurationException On parser configuration errors
   * @throws IOException                  On I/O errors
   * @throws SAXException                 On parse errors
   */

  public static Document xmlParseFromStream(
    final Path path,
    final InputStream stream)
    throws ParserConfigurationException, SAXException, IOException
  {
    Objects.requireNonNull(path, "path");
    Objects.requireNonNull(stream, "stream");

    final DocumentBuilderFactory factory =
      DocumentBuilderFactory.newDefaultInstance();
    factory.setValidating(false);
    factory.setFeature(
      XMLConstants.FEATURE_SECURE_PROCESSING, true);
    factory.setFeature(
      "http://apache.org/xml/features/nonvalidating/load-external-dtd",
      false);
    factory.setFeature(
      "http://apache.org/xml/features/xinclude", false);
    factory.setFeature(
      "http://xml.org/sax/features/namespaces", true);
    factory.setFeature(
      "http://xml.org/sax/features/validation", false);
    factory.setFeature(
      "http://apache.org/xml/features/validation/schema", false);

    final DocumentBuilder builder = factory.newDocumentBuilder();
    return builder.parse(stream, path.toString());
  }
}
