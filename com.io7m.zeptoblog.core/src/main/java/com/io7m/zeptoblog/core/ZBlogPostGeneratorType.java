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

import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.control.Validation;
import org.osgi.annotation.versioning.ProviderType;

import java.nio.file.Path;
import java.util.Properties;

/**
 * The type of blog post generators.
 */

@ProviderType
public interface ZBlogPostGeneratorType extends ZServiceType
{
  /**
   * Generate a set of blog posts.
   *
   * @param config The blog configuration
   * @param props  Implementation-specific properties
   *
   * @return A set of blog posts, or a list of generation errors on failure
   */

  Validation<Seq<ZError>, SortedMap<Path, ZBlogPost>> generate(
    ZBlogConfiguration config,
    Properties props);
}
