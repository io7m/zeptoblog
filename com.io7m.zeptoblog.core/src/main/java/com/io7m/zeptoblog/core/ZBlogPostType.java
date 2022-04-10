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

import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * The type of blog posts.
 */

@ZImmutableStyleType
@VavrEncodingEnabled
@Value.Immutable
public interface ZBlogPostType extends Comparable<ZBlogPostType>
{
  /**
   * @return The blog post title
   */

  @Value.Parameter
  String title();

  /**
   * @return The blog post publication date
   */

  @Value.Parameter
  Optional<ZonedDateTime> date();

  /**
   * @return The path to the blog post
   */

  @Value.Parameter
  Path path();

  /**
   * @return The body of the post
   */

  @Value.Parameter
  ZBlogPostBody body();

  /**
   * @param config The blog configuration
   *
   * @return A path to the generated permalink file
   */

  default Path outputPermalinkFileAbsolute(
    final ZBlogConfiguration config)
  {
    final Path source_zbp = this.path();

    final Path source_xhtml;
    final Path parent = source_zbp.getParent();
    final Path file_name = source_zbp.getFileName();

    if (file_name == null) {
      throw new IllegalStateException(
        "Could not resolve a filename for path: " + source_zbp);
    }

    if (parent != null) {
      source_xhtml =
        parent.resolve(file_name.toString()
                         .replaceAll("\\.zbp", ".xhtml"));
    } else {
      source_xhtml =
        source_zbp.resolveSibling(
          file_name.toString()
            .replaceAll("\\.zbp", ".xhtml"));
    }

    return config.outputRoot().resolve(source_xhtml).toAbsolutePath();
  }

  /**
   * @param config The blog configuration
   *
   * @return A link to the generated permalink file
   */

  default Path outputPermalinkFileLink(
    final ZBlogConfiguration config)
  {
    return config.outputRoot().relativize(
      this.outputPermalinkFileAbsolute(config));
  }

  /**
   * @param config The blog configuration
   *
   * @return An absolute link to the generated permalink file
   */

  default String outputPermalinkLink(
    final ZBlogConfiguration config)
  {
    return "/" + this.outputPermalinkFileLink(config);
  }

  @Override
  default int compareTo(final ZBlogPostType other)
  {
    Objects.requireNonNull(other, "other");

    final Optional<ZonedDateTime> a_opt = this.date();
    final Optional<ZonedDateTime> b_opt = other.date();
    if (a_opt.isPresent()) {
      if (b_opt.isPresent()) {
        final ZonedDateTime a_date = a_opt.get();
        final ZonedDateTime b_date = b_opt.get();
        return a_date.compareTo(b_date);
      }
      return 1;
    }

    if (b_opt.isPresent()) {
      return -1;
    }

    return 0;
  }
}
