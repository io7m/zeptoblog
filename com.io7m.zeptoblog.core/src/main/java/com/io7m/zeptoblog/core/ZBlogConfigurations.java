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

import com.io7m.jlexing.core.LexicalPosition;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.junreachable.UnreachableCodeException;
import io.vavr.collection.Seq;
import io.vavr.collection.Vector;
import io.vavr.control.Validation;

import java.math.BigInteger;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

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
    Objects.requireNonNull(path, "Path");
    Objects.requireNonNull(p, "Properties");

    final ZBlogConfiguration.Builder builder = ZBlogConfiguration.builder();
    final FileSystem fs = path.getFileSystem();

    Vector<ZError> errors = Vector.empty();
    errors = configureProperties(path, p, builder, fs, errors);
    configureFooter(p, builder, fs);
    configureHeader(p, builder, fs);
    errors = configureGenerators(path, p, builder, errors, fs);
    return validate(builder, errors);
  }

  private static Vector<ZError> property(
    final Vector<ZError> errors_initial,
    final Path path,
    final Properties p,
    final String name,
    final Consumer<String> consumer)
  {
    try {
      consumer.accept(JProperties.getString(p, name));
    } catch (final Exception e) {
      return errors_initial.append(ofException(path, e));
    }
    return errors_initial;
  }

  private static Vector<ZError> configureProperties(
    final Path path,
    final Properties p,
    final ZBlogConfiguration.Builder builder,
    final FileSystem fs,
    final Vector<ZError> errors_initial)
  {
    Vector<ZError> errors = errors_initial;

    errors = property(errors, path, p, "com.io7m.zeptoblog.title", value -> builder.setTitle(value));

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

    errors = property(errors, path, p, "com.io7m.zeptoblog.author", value -> builder.setAuthor(value));
    errors = property(errors, path, p, "com.io7m.zeptoblog.format_default", value -> builder.setFormatDefault(value));

    try {
      builder.setPostsPerPage(
        JProperties.getBigIntegerWithDefault(
          p,
          "com.io7m.zeptoblog.posts_per_page",
          BigInteger.TEN).intValueExact());
    } catch (final Exception e) {
      errors = errors.append(ofException(path, e));
    }
    return errors;
  }

  private static Validation<Seq<ZError>, ZBlogConfiguration> validate(
    final ZBlogConfiguration.Builder builder,
    final Vector<ZError> errors)
  {
    if (errors.isEmpty()) {
      return Validation.valid(builder.build());
    }
    return Validation.invalid(errors);
  }

  private static void configureFooter(
    final Properties p,
    final ZBlogConfiguration.Builder builder,
    final FileSystem fs)
  {
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
  }

  private static void configureHeader(
    final Properties p,
    final ZBlogConfiguration.Builder builder,
    final FileSystem fs)
  {
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
  }

  private static Vector<ZError> configureGenerators(
    final Path path,
    final Properties p,
    final ZBlogConfiguration.Builder builder,
    final Vector<ZError> errors_initial,
    final FileSystem fs)
  {
    Vector<ZError> errors = errors_initial;

    try {
      final String generators =
        JProperties.getStringWithDefault(
          p, "com.io7m.zeptoblog.generators", "")
          .trim();

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
    return errors;
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
