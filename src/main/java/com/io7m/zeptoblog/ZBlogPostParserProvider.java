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

import com.io7m.jlexing.core.LexicalPositionMutable;
import com.io7m.jnull.NullCheck;
import javaslang.collection.Seq;
import javaslang.collection.SortedSet;
import javaslang.collection.TreeSet;
import javaslang.collection.Vector;
import javaslang.control.Validation;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.osgi.service.component.annotations.Component;
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

import static javaslang.control.Validation.invalid;
import static javaslang.control.Validation.valid;

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

  /**
   * Construct a blog post parser provider.
   */

  public ZBlogPostParserProvider()
  {

  }

  @Override
  public ZBlogPostParserType createParser(
    final InputStream stream,
    final Path path)
  {
    return new Parser(stream, path);
  }

  private static final class Parser implements ZBlogPostParserType
  {
    private final InputStream stream;
    private final LexicalPositionMutable<Path> position;
    private final DateTimeFormatter formatter;
    private final Path path;
    private Vector<ZError> errors;
    private String title;
    private ZonedDateTime date;
    private SortedSet<String> tags;

    Parser(
      final InputStream in_stream,
      final Path in_path)
    {
      this.stream = NullCheck.notNull(in_stream, "stream");
      this.path = NullCheck.notNull(in_path, "path");
      this.position = LexicalPositionMutable.create(0, 0, Optional.of(in_path));
      this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
      this.errors = Vector.empty();
      this.tags = TreeSet.empty();
    }

    @Override
    public Validation<Seq<ZError>, ZBlogPost> parse()
    {
      try (final BufferedReader reader =
             new BufferedReader(
               new InputStreamReader(
                 new CloseShieldInputStream(this.stream),
                 StandardCharsets.UTF_8))) {

        this.parseHeader(reader);

        if (!this.errors.isEmpty()) {
          return invalid(this.errors);
        }

        final String body = this.parseBody(reader);

        if (LOG.isDebugEnabled()) {
          LOG.debug("file:  {}", this.path);
          LOG.debug("date:  {}", this.date.format(this.formatter));
          LOG.debug("title: {}", this.title);
        }

        return valid(ZBlogPost.of(
          this.title,
          this.date,
          this.tags,
          body,
          this.path));
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
      if (this.date == null) {
        this.fail("Date not specified", Optional.empty());
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
        case "tags": {
          this.parseHeaderCommandTags(tokens);
          break;
        }
        default: {
          this.fail("Unrecognized command", Optional.empty());
          break;
        }
      }
    }

    private void parseHeaderCommandTags(
      final Vector<String> tokens)
    {
      this.tags = TreeSet.ofAll(tokens.tail());
    }

    private void parseHeaderCommandDate(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() == 2) {
        try {
          this.date = ZonedDateTime.parse(tokens.get(1), this.formatter);
        } catch (final Exception e) {
          this.fail(e.getMessage(), Optional.of(e));
        }
      } else {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Syntax error.");
        sb.append(System.lineSeparator());
        sb.append("  Expected: date <date>");
        sb.append(System.lineSeparator());
        sb.append("  Received: ");
        sb.append(line);
        sb.append(System.lineSeparator());
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
        sb.append("Syntax error.");
        sb.append(System.lineSeparator());
        sb.append("  Expected: title <text> <text>*");
        sb.append(System.lineSeparator());
        sb.append("  Received: ");
        sb.append(line);
        sb.append(System.lineSeparator());
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
