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

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jnull.NullCheck;
import javaslang.collection.Seq;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import javaslang.control.Validation;
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
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * The default blog parser provider.
 */

@Component(service = ZBlogParserProviderType.class)
public final class ZBlogParserProvider implements ZBlogParserProviderType
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogParserProvider.class);
  }

  private final AtomicReference<ZBlogPostParserProviderType> post_provider;

  /**
   * Construct a blog parser provider.
   */

  public ZBlogParserProvider()
  {
    this.post_provider = new AtomicReference<>();
  }

  /**
   * Introduce a blog post parser provider.
   *
   * @param provider The provider
   */

  @Reference(
    policyOption = ReferencePolicyOption.RELUCTANT,
    policy = ReferencePolicy.STATIC,
    cardinality = ReferenceCardinality.MANDATORY,
    unbind = "unsetBlogPostParserProvider")
  public void setBlogPostParserProvider(
    final ZBlogPostParserProviderType provider)
  {
    this.post_provider.set(NullCheck.notNull(provider, "provider"));
  }

  /**
   * Remove a blog post parser provider.
   *
   * @param provider The provider
   */

  public void unsetBlogPostParserProvider(
    final ZBlogPostParserProviderType provider)
  {
    this.post_provider.compareAndSet(provider, null);
  }

  @Override
  public ZBlogParserType createParser(
    final ZBlogConfiguration config)
  {
    NullCheck.notNull(config, "config");
    return new Parser(this.post_provider.get(), config);
  }

  private static final class Parser implements ZBlogParserType,
    FileVisitor<Path>
  {
    private final ZBlogConfiguration config;
    private final ZBlog.Builder builder;
    private final ZBlogPostParserProviderType provider;
    private final DateTimeFormatter formatter;
    private TreeMap<String, ZBlogPost> posts;
    private Vector<ZError> errors;

    Parser(
      final ZBlogPostParserProviderType in_provider,
      final ZBlogConfiguration in_config)
    {
      this.provider = NullCheck.notNull(in_provider, "Provider");
      this.config = NullCheck.notNull(in_config, "Config");
      this.errors = Vector.empty();
      this.builder = ZBlog.builder();
      this.builder.setTitle(in_config.title());
      this.posts = TreeMap.empty();
      this.formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");
    }

    @Override
    public Validation<Seq<ZError>, ZBlog> parse()
    {
      try {
        Files.walkFileTree(
          this.config.sourceRoot(),
          EnumSet.noneOf(FileVisitOption.class),
          Integer.MAX_VALUE,
          this);
        this.builder.setPosts(this.posts);
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
        return Validation.valid(this.builder.build());
      }
      return Validation.invalid(this.errors);
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
      if (extension != null && Objects.equals(extension, "zbp")) {
        LOG.debug("parsing {}", file);

        try (final InputStream stream = Files.newInputStream(file)) {
          final Path relative = this.config.sourceRoot().relativize(file);

          final ZBlogPostParserType parser =
            this.provider.createParser(stream, relative);
          final Validation<Seq<ZError>, ZBlogPost> r = parser.parse();
          if (r.isInvalid()) {
            this.errors.appendAll(r.getError());
          } else {
            final ZBlogPost post = r.get();

            if (this.posts.containsKey(post.id())) {
              final StringBuilder sb = new StringBuilder(128);
              sb.append("Duplicate blog post ID.");
              sb.append(System.lineSeparator());
              sb.append("  Post title: ");
              sb.append(post.title());
              sb.append(System.lineSeparator());
              sb.append("  Post date:  ");
              sb.append(post.date().format(this.formatter));
              sb.append(System.lineSeparator());
              sb.append("  Post ID:    ");
              sb.append(post.id());
              sb.append(System.lineSeparator());
              this.errors.append(ZError.of(
                sb.toString(),
                LexicalPosition.of(0, 0, Optional.of(file)),
                Optional.empty()));
            } else {
              this.posts = this.posts.put(post.id(), post);
            }
          }
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
      throws IOException
    {
      return FileVisitResult.CONTINUE;
    }
  }
}
