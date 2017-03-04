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
import com.io7m.jnull.NullCheck;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.junreachable.UnreachableCodeException;
import javaslang.collection.Seq;
import javaslang.collection.Vector;
import javaslang.control.Validation;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

/**
 * Functions for producing blog configurations.
 */

public final class ZBlogConfigurations
{
  private ZBlogConfigurations()
  {
    throw new UnreachableCodeException();
  }

  /**
   * Parse the given properties, yielding a blog configuration.
   *
   * @param path The path to the properties file, for error messages
   * @param p    The properties
   *
   * @return A parsed blog configuration, or a list of errors
   */

  public static Validation<Seq<ZError>, ZBlogConfiguration> fromProperties(
    final Path path,
    final Properties p)
  {
    NullCheck.notNull(path, "Path");
    NullCheck.notNull(p, "Properties");

    final ZBlogConfiguration.Builder builder = ZBlogConfiguration.builder();

    Vector<ZError> errors = Vector.empty();

    try {
      builder.setTitle(
        JProperties.getString(p, "com.io7m.zeptoblog.title"));
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    final FileSystem fs = path.getFileSystem();

    try {
      builder.setSourceRoot(fs.getPath(JProperties.getString(
        p,
        "com.io7m.zeptoblog.source_root")).toAbsolutePath());
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setOutputRoot(
        fs.getPath(JProperties.getString(
          p,
          "com.io7m.zeptoblog.output_root")).toAbsolutePath());
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setSiteURI(
        new URI(JProperties.getString(p, "com.io7m.zeptoblog.site_uri")));
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setAuthor(
        JProperties.getString(p, "com.io7m.zeptoblog.author"));
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setFormatDefault(
        JProperties.getString(p, "com.io7m.zeptoblog.format_default"));
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setPostsPerPage(
        JProperties.getBigIntegerOptional(
          p,
          "com.io7m.zeptoblog.posts_per_page",
          BigInteger.TEN).intValueExact());
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }

    try {
      builder.setFooterPre(fs.getPath(
        JProperties.getString(p, "com.io7m.zeptoblog.footer_pre")));
    } catch (final JPropertyNonexistent e) {
      // Ignore
    }

    try {
      builder.setFooterPost(fs.getPath(
        JProperties.getString(p, "com.io7m.zeptoblog.footer_post")));
    } catch (final JPropertyNonexistent e) {
      // Ignore
    }

    try {
      builder.setHeaderReplace(fs.getPath(
        JProperties.getString(p, "com.io7m.zeptoblog.header_replace")));
    } catch (final JPropertyNonexistent e) {
      // Ignore
    }

    try {
      builder.setHeaderPre(fs.getPath(
        JProperties.getString(p, "com.io7m.zeptoblog.header_pre")));
    } catch (final JPropertyNonexistent e) {
      // Ignore
    }

    try {
      builder.setHeaderPost(fs.getPath(
        JProperties.getString(p, "com.io7m.zeptoblog.header_post")));
    } catch (final JPropertyNonexistent e) {
      // Ignore
    }


    try {
      String generators = "";
      try {
        generators =
          JProperties.getStringOptional(
            p, "com.io7m.zeptoblog.generators", "").trim();
      } catch (final JPropertyNonexistent ex) {
        // Ignore
      }

      if (!generators.isEmpty()) {
        final String[] names = generators.split("\\s+");
        for (int index = 0; index < names.length; ++index) {
          final String name = names[index];
          final String gen_type = JProperties.getString(
            p, String.format("com.io7m.zeptoblog.generators.%s.type", name));
          final Path gen_file = fs.getPath(JProperties.getString(
            p, String.format("com.io7m.zeptoblog.generators.%s.file", name)));
          builder.putGeneratorRequests(
            name, ZBlogPostGeneratorRequest.of(name, gen_type, gen_file));
        }
      }
    } catch (final JPropertyNonexistent ex) {
      errors = errors.append(ofException(path, ex));
    }

    if (errors.isEmpty()) {
      return Validation.valid(builder.build());
    }
    return Validation.invalid(errors);
  }

  private static ZError ofException(
    final Path path,
    final Exception e)
  {
    return ZError.of(
      e.getMessage(),
      LexicalPosition.of(0, 0, Optional.of(path)),
      Optional.of(e));
  }
}
