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

package com.io7m.zeptoblog.glossary;

import com.io7m.jlexing.core.LexicalPositionMutable;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogPostFormatResolverSL;
import com.io7m.zeptoblog.core.ZBlogPostFormatResolverType;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.HashSet;
import io.vavr.collection.Seq;
import io.vavr.collection.Set;
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
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;

/**
 * The default glossary item parser provider.
 */

@Component(service = ZGlossaryItemParserProviderType.class)
public final class ZGlossaryItemParserProvider implements
  ZGlossaryItemParserProviderType
{
  private static final Logger LOG;
  private static final String LINE_SEPARATOR = System.lineSeparator();

  static {
    LOG = LoggerFactory.getLogger(ZGlossaryItemParserProvider.class);
  }

  private volatile ZBlogPostFormatResolverType resolver;

  /**
   * Construct a blog post parser provider.
   */

  public ZGlossaryItemParserProvider()
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
    this.resolver = Objects.requireNonNull(in_resolver, "Resolver");
  }

  @Override
  public ZGlossaryItemParserType createParser(
    final ZBlogConfiguration config,
    final InputStream stream,
    final Path path)
  {
    return new Parser(config, stream, path);
  }

  private static final class Parser implements ZGlossaryItemParserType
  {
    private final InputStream stream;
    private final LexicalPositionMutable<Path> position;
    private final Path path;
    private final ZBlogConfiguration config;
    private Vector<ZError> errors;
    private String term;
    private String format_name;
    private Set<String> related;

    Parser(
      final ZBlogConfiguration in_config,
      final InputStream in_stream,
      final Path in_path)
    {
      this.config = Objects.requireNonNull(in_config, "Config");
      this.stream = Objects.requireNonNull(in_stream, "stream");
      this.path = Objects.requireNonNull(in_path, "path");

      this.position = LexicalPositionMutable.create(0, 0, Optional.of(in_path));
      this.errors = Vector.empty();
      this.related = HashSet.empty();
    }

    @Override
    public Validation<Seq<ZError>, ZGlossaryItem> parse()
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
          LOG.debug("file: {}", this.path);
          LOG.debug("term: {}", this.term);
        }

        if (this.format_name == null) {
          this.format_name = this.config.formatDefault();
        }

        return valid(ZGlossaryItem.of(
          this.term,
          this.related,
          ZGlossaryItemBody.of(this.format_name, body_text),
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

      if (this.term == null) {
        this.fail("Term not specified", Optional.empty());
      }
    }

    private void parseHeaderCommand(
      final String line)
    {
      final Vector<String> tokens = Vector.of(line.split("\\s+"));
      switch (tokens.get(0)) {
        case "term": {
          this.parseHeaderCommandTerm(line, tokens);
          break;
        }
        case "format": {
          this.parseHeaderCommandFormat(line, tokens);
          break;
        }
        case "related": {
          this.parseHeaderCommandRelated(line, tokens);
          break;
        }
        default: {
          this.fail("Unrecognized command", Optional.empty());
          break;
        }
      }
    }

    private void parseHeaderCommandRelated(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() >= 2) {
        this.related = HashSet.ofAll(tokens.tail());
      } else {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Syntax error.");
        sb.append(LINE_SEPARATOR);
        sb.append("  Expected: related <term> <term>*");
        sb.append(LINE_SEPARATOR);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(LINE_SEPARATOR);
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
        sb.append("Syntax error.");
        sb.append(LINE_SEPARATOR);
        sb.append("  Expected: format <format-name>");
        sb.append(LINE_SEPARATOR);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(LINE_SEPARATOR);
        this.fail(sb.toString(), Optional.empty());
      }
    }

    private void parseHeaderCommandTerm(
      final String line,
      final Vector<String> tokens)
    {
      if (tokens.size() >= 2) {
        this.term = tokens.tail().collect(Collectors.joining(" "));
      } else {
        final StringBuilder sb = new StringBuilder(128);
        sb.append("Syntax error.");
        sb.append(LINE_SEPARATOR);
        sb.append("  Expected: term <text> <text>*");
        sb.append(LINE_SEPARATOR);
        sb.append("  Received: ");
        sb.append(line);
        sb.append(LINE_SEPARATOR);
        this.fail(sb.toString(), Optional.empty());
      }
    }

    private Validation<Seq<ZError>, ZGlossaryItem> fail(
      final String message,
      final Optional<Exception> exception)
    {
      this.errors = this.errors.append(
        ZError.of(message, this.position.toImmutable(), exception));
      return invalid(this.errors);
    }
  }
}
