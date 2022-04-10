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

import com.io7m.jlexing.core.LexicalPositionMutable;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;

/**
 * The default blog post parser provider.
 */

@Component(service = ZBlogPostParserProviderType.class)
public final class ZBlogPostParserProvider implements
  ZBlogPostParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogPostParserProvider.class);
  }

  private volatile ZBlogPostFormatResolverType resolver;

  /**
   * Construct a blog post parser provider.
   */

  public ZBlogPostParserProvider()
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
    this.resolver = Objects.requireNonNull(in_resolver, "resolver");
  }

  @Override
  public ZBlogPostParserType createParser(
    final ZBlogConfiguration config,
    final InputStream stream,
    final Path path)
  {
    return new Parser(config, stream, path);
  }

  private static final class Parser implements ZBlogPostParserType
  {
    private final InputStream stream;
    private final LexicalPositionMutable<Path> position;
    private final DateTimeFormatter formatter;
    private final Path path;
    private final ZBlogConfiguration config;
    private Vector<ZError> errors;
    private String title;
    private Optional<ZonedDateTime> date;
    private String format_name;

    Parser(
      final ZBlogConfiguration in_config,
      final InputStream in_stream,
      final Path in_path)
    {
      this.config = Objects.requireNonNull(in_config, "config");
      this.stream = Objects.requireNonNull(in_stream, "stream");
      this.path = Objects.requireNonNull(in_path, "path");

      this.position = LexicalPositionMutable.create(0, 0, Optional.of(in_path));
      this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
      this.errors = Vector.empty();
      this.date = Optional.empty();
    }

    @Override
    public Validation<Seq<ZError>, ZBlogPost> parse()
    {
      try (BufferedReader reader =
             new BufferedReader(
               new InputStreamReader(
                 new CloseShieldInputStream(this.stream),
                 StandardCharsets.UTF_8))) {

        this.parseHeader(reader);

        if (!this.errors.isEmpty()) {
          return invalid(this.errors);
        }

        final String body_text = this.parseBody(reader);

        if (LOG.isDebugEnabled()) {
          LOG.debug("file:  {}", this.path);
          this.date.ifPresent(post_date -> LOG.debug(
            "date:  {}",
            post_date.format(this.formatter)));
          LOG.debug("title: {}", this.title);
        }

        if (this.format_name == null) {
          this.format_name = this.config.formatDefault();
        }

        return valid(ZBlogPost.of(
          this.title,
          this.date,
          this.path,
          ZBlogPostBody.of(this.format_name, body_text)));
      } catch (final IOException e) {
        return this.fail("I/O error: " + e.getMessage(), Optional.of(e));
      }
    }

    private String parseBody(
      final BufferedReader reader)
      throws IOException
    {
      final StringBuilder sb = new StringBuilder(128);
      while (true) {
        this.position.setLine(this.position.line() + 1);

        final String raw_line = reader.readLine();
        if (raw_line == null) {
          break;
        }
        sb.append(raw_line);
        sb.append(System.lineSeparator());
      }

      return sb.toString();
    }

    private void parseHeader(
      final BufferedReader reader)
      throws IOException
    {
      while (true) {
        this.position.setLine(this.position.line() + 1);

        final String raw_line = reader.readLine();
        if (raw_line == null) {
          this.fail("Unexpected EOF", Optional.empty());
          break;
        }
        final String line = raw_line.trim();
        if (Objects.equals(line, "")) {
          break;
        }
        this.parseHeaderCommand(line);
      }

      if (this.title == null) {
        this.fail("Title not specified", Optional.empty());
      }
    }

    private void parseHeaderCommand(
      final String line)
    {
      final Vector<String> tokens = Vector.of(line.split("\\s+"));
      switch (tokens.get(0)) {
        case "title": {
          this.parseHeaderCommandTitle(line, tokens);
          break;
        }
        case "date": {
          this.parseHeaderCommandDate(line, tokens);
          break;
        }
        case "format": {
          this.parseHeaderCommandFormat(line, tokens);
          break;
        }
        default: {
          this.fail("Unrecognized command", Optional.empty());
          break;
        }
      }
    }

    private void parseHeaderCommandDate(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() == 2) {
        try {
          this.date =
            Optional.of(ZonedDateTime.parse(tokens.get(1), this.formatter));
        } catch (final Exception e) {
          this.fail(e.getMessage(), Optional.of(e));
        }
      } else {
        final String separator = System.lineSeparator();
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Syntax error.");
        sb.append(separator);
        sb.append("  Expected: date <date>");
        sb.append(separator);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(separator);
        this.fail(sb.toString(), Optional.empty());
      }
    }

    private void parseHeaderCommandFormat(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() == 2) {
        this.format_name = tokens.get(1);
      } else {
        final StringBuilder sb = new StringBuilder(128);
        final String separator = System.lineSeparator();
        sb.append("Syntax error.");
        sb.append(separator);
        sb.append("  Expected: format <format-name>");
        sb.append(separator);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(separator);
        this.fail(sb.toString(), Optional.empty());
      }
    }

    private void parseHeaderCommandTitle(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() >= 2) {
        this.title = tokens.tail().collect(Collectors.joining(" "));
      } else {
        final StringBuilder sb = new StringBuilder(128);
        final String separator = System.lineSeparator();
        sb.append("Syntax error.");
        sb.append(separator);
        sb.append("  Expected: title <text> <text>*");
        sb.append(separator);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(separator);
        this.fail(sb.toString(), Optional.empty());
      }
    }

    private Validation<Seq<ZError>, ZBlogPost> fail(
      final String message,
      final Optional<Exception> exception)
    {
      this.errors = this.errors.append(
        ZError.of(message, this.position.toImmutable(), exception));
      return invalid(this.errors);
    }
  }
}
