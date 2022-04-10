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

import com.io7m.jaffirm.core.Preconditions;
import io.vavr.collection.Map;
import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.net.URI;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The type of blog configurations.
 */

@ZImmutableStyleType
@VavrEncodingEnabled
@Value.Immutable
public interface ZBlogConfigurationType
{
  /**
   * @return The blog title
   */

  @Value.Parameter
  String title();

  /**
   * @return The blog author
   */

  @Value.Parameter
  String author();

  /**
   * @return The directory containing source files for the blog
   */

  @Value.Parameter
  Path sourceRoot();

  /**
   * @return The directory to which output files will be written
   */

  @Value.Parameter
  Path outputRoot();

  /**
   * @return The number of posts per page
   */

  @Value.Parameter
  int postsPerPage();

  /**
   * @return The site URI
   */

  @Value.Parameter
  URI siteURI();

  /**
   * @return The default format to use for blog posts
   */

  @Value.Parameter
  String formatDefault();

  /**
   * @return An optional file containing an XHTML element that will be used to
   * replace the blog header
   */

  @Value.Parameter
  Optional<Path> headerReplace();

  /**
   * @return An optional file containing an XHTML element that will be prepended
   * to the blog header
   */

  @Value.Parameter
  Optional<Path> headerPre();

  /**
   * @return An optional file containing an XHTML element that will be appended
   * to the blog header
   */

  @Value.Parameter
  Optional<Path> headerPost();

  /**
   * @return An optional file containing an XHTML element that will be prepended
   * to the blog footer
   */

  @Value.Parameter
  Optional<Path> footerPre();

  /**
   * @return An optional file containing an XHTML element that will be appended
   * to the blog footer
   */

  @Value.Parameter
  Optional<Path> footerPost();

  /**
   * @return The set of requests to execute generators
   */

  @Value.Parameter
  Map<String, ZBlogPostGeneratorRequest> generatorRequests();

  /**
   * Check preconditions for the type.
   */

  @Value.Check
  default void checkPreconditions()
  {
    Preconditions.checkPrecondition(
      this.sourceRoot(),
      this.sourceRoot().isAbsolute(),
      p -> "Source root path " + p + " must be absolute");

    Preconditions.checkPrecondition(
      this.outputRoot(),
      this.outputRoot().isAbsolute(),
      p -> "Output root path " + p + " must be absolute");
  }
}
