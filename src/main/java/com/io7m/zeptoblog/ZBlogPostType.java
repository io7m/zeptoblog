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

import com.io7m.jnull.NullCheck;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.SortedSet;
import org.apache.commons.codec.binary.Hex;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The type of blog posts.
 */

@ZImmutableStyleType
@JavaslangEncodingEnabled
@Value.Immutable
public interface ZBlogPostType extends Comparable<ZBlogPostType>
{
  /**
   * @return The unique ID for the blog, based on the publication date and title
   */

  @Value.Lazy
  default String id()
  {
    try {
      final DateTimeFormatter formatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ");

      final MessageDigest digest = MessageDigest.getInstance("SHA-256");
      final Charset cs = StandardCharsets.UTF_8;
      digest.update(this.title().getBytes(cs));
      digest.update(":".getBytes(cs));
      digest.update(formatter.format(this.date()).getBytes(cs));
      return Hex.encodeHexString(digest.digest());
    } catch (final NoSuchAlgorithmException e) {
      throw new UnreachableCodeException(e);
    }
  }

  /**
   * @param config The blog configuration
   *
   * @return A shortened ID based on the given config
   */

  default String idShort(
    final ZBlogConfiguration config)
  {
    final String text = this.id();
    return text.substring(0, Math.min(text.length(), config.idLength()));
  }

  /**
   * @return The blog post title
   */

  @Value.Parameter
  String title();

  /**
   * @return The blog post publication date
   */

  @Value.Parameter
  ZonedDateTime date();

  /**
   * @return The blog post tags
   */

  @Value.Parameter
  SortedSet<String> tags();

  /**
   * @return The blog post body text
   */

  @Value.Parameter
  String body();

  /**
   * @return The path to the blog post
   */

  @Value.Parameter
  Path path();

  /**
   * @param config The blog configuration
   *
   * @return A path to the generated permalink file
   */

  default Path outputPermalinkFileAbsolute(
    final ZBlogConfiguration config)
  {
    final Path source_zbp = this.path();
    final Path source_xhtml =
      source_zbp.getParent().resolve(
        source_zbp.getFileName().toString().replaceAll("\\.zbp", ".xhtml"));
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
    return config.outputRoot().relativize(this.outputPermalinkFileAbsolute(
      config));
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
  default int compareTo(final ZBlogPostType o)
  {
    return this.date().compareTo(NullCheck.notNull(o, "Date").date());
  }
}
