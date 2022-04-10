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

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.collection.TreeMap;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;
import org.apache.commons.io.FilenameUtils;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;

import static io.vavr.control.Validation.invalid;
import static io.vavr.control.Validation.valid;

/**
 * The default glossary parser provider.
 */

@Component(service = ZGlossaryParserProviderType.class)
public final class ZGlossaryParserProvider
  implements ZGlossaryParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZGlossaryParserProvider.class);
  }

  private volatile ZGlossaryItemParserProviderType item_provider;

  /**
   * Construct a glossary parser provider.
   */

  public ZGlossaryParserProvider()
  {
    this.item_provider = new ZGlossaryItemParserProvider();
  }

  /**
   * Introduce a glossary item parser provider.
   *
   * @param provider The item parser provider
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY)
  public void setGlossaryItemParserProvider(
    final ZGlossaryItemParserProviderType provider)
  {
    this.item_provider = Objects.requireNonNull(provider, "Item provider");
  }

  @Override
  public ZGlossaryParserType createParser(
    final ZBlogConfiguration config,
    final Path path)
  {
    Objects.requireNonNull(config, "config");
    Objects.requireNonNull(path, "path");
    return new Parser(this.item_provider, config, path);
  }

  private static final class Parser implements ZGlossaryParserType,
    FileVisitor<Path>
  {
    private final ZBlogConfiguration config;
    private final ZGlossary.Builder builder;
    private final ZGlossaryItemParserProviderType item_provider;
    private final Path path;
    private TreeMap<String, ZGlossaryItem> items;
    private Vector<ZError> errors;

    Parser(
      final ZGlossaryItemParserProviderType in_item_provider,
      final ZBlogConfiguration in_config,
      final Path in_path)
    {
      this.item_provider =
        Objects.requireNonNull(in_item_provider, "Post provider");
      this.config =
        Objects.requireNonNull(in_config, "Config");
      this.path =
        Objects.requireNonNull(in_path, "Path");

      this.errors = Vector.empty();
      this.builder = ZGlossary.builder();
      this.items = TreeMap.empty();
    }

    @Override
    public Validation<Seq<ZError>, ZGlossary> parse()
    {
      try {
        Files.walkFileTree(
          this.path,
          EnumSet.noneOf(FileVisitOption.class),
          Integer.MAX_VALUE,
          this);
        this.builder.setItems(this.items);
      } catch (final NoSuchFileException e) {
        this.errors = this.errors.append(ZError.of(
          "No such file: " + e.getMessage(),
          LexicalPosition.of(0, 0, Optional.empty()),
          Optional.of(e)));
      } catch (final IOException e) {
        this.errors = this.errors.append(ZError.of(
          "I/O error: " + e.getMessage(),
          LexicalPosition.of(0, 0, Optional.empty()),
          Optional.of(e)));
      }

      if (this.errors.isEmpty()) {
        return valid(this.builder.build());
      }
      return invalid(this.errors);
    }

    @Override
    public FileVisitResult preVisitDirectory(
      final Path dir,
      final BasicFileAttributes attrs)
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
      if (extension != null) {
        if (Objects.equals(extension, "zbp")) {
          this.parsePost(file);
        }
      }

      return FileVisitResult.CONTINUE;
    }

    private void parsePost(
      final Path file)
      throws IOException
    {
      LOG.debug("parsing item {}", file);

      try (InputStream stream = Files.newInputStream(file)) {
        final Path absolute = file.toAbsolutePath();
        final Path relative = this.config.sourceRoot().relativize(absolute);

        final ZGlossaryItemParserType parser =
          this.item_provider.createParser(this.config, stream, relative);
        final Validation<Seq<ZError>, ZGlossaryItem> r = parser.parse();
        if (r.isInvalid()) {
          this.errors = this.errors.appendAll(r.getError());
        } else {
          final ZGlossaryItem item = r.get();
          final String item_term = item.term();
          if (this.items.containsKey(item_term)) {
            final StringBuilder sb = new StringBuilder(128);
            sb.append("Duplicate glossary item.");
            final String separator = System.lineSeparator();
            sb.append(separator);
            sb.append("  Post term: ");
            sb.append(item_term);
            sb.append(separator);
            this.errors.append(ZError.of(
              sb.toString(),
              LexicalPosition.of(0, 0, Optional.of(file)),
              Optional.empty()));
          } else {
            this.items = this.items.put(item_term, item);
          }
        }
      }
    }

    @Override
    public FileVisitResult visitFileFailed(
      final Path file,
      final IOException exc)
    {
      if (exc instanceof NoSuchFileException) {
        this.errors = this.errors.append(ZError.of(
          "No such file: " + exc.getMessage(),
          LexicalPosition.of(0, 0, Optional.of(file.toAbsolutePath())),
          Optional.of(exc)));
      } else {
        this.errors = this.errors.append(ZError.of(
          "I/O error: " + exc.getMessage(),
          LexicalPosition.of(0, 0, Optional.of(file.toAbsolutePath())),
          Optional.of(exc)));
      }

      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(
      final Path dir,
      final IOException exc)
    {
      return FileVisitResult.CONTINUE;
    }
  }
}
