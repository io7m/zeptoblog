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

import javaslang.Tuple2;
import javaslang.collection.Seq;
import javaslang.collection.SortedMap;
import javaslang.collection.TreeMap;
import javaslang.collection.Vector;
import org.immutables.javaslang.encodings.JavaslangEncodingEnabled;
import org.immutables.value.Value;

import java.time.ZonedDateTime;
import java.time.chrono.ChronoZonedDateTime;
import java.util.function.Function;

/**
 * The type of blogs.
 */

@ZImmutableStyleType
@JavaslangEncodingEnabled
@Value.Immutable
public interface ZBlogType
{
  /**
   * @return The blog title
   */

  @Value.Parameter
  String title();

  /**
   * @return The blog posts by ID
   */

  @Value.Parameter
  SortedMap<String, ZBlogPost> posts();

  /**
   * @return The blog posts by date
   */

  @Value.Lazy
  default SortedMap<ZonedDateTime, ZBlogPost> postsByDate()
  {
    return this.posts().values().toSortedMap(
      ChronoZonedDateTime::compareTo, ZBlogPost::date, Function.identity());
  }

  /**
   * Group the blog posts by page.
   *
   * @param count The number of posts per page
   *
   * @return A list of pages containing posts
   */

  default SortedMap<Integer, Seq<ZBlogPost>> postsByPage(
    final int count)
  {
    SortedMap<Integer, Seq<ZBlogPost>> result = TreeMap.empty();

    int page_number = 0;
    Vector<ZBlogPost> page = Vector.empty();
    for (final Tuple2<ZonedDateTime, ZBlogPost> pair : this.postsByDate()) {
      final int page_size = page.size();
      if (page_size == count) {
        result = result.put(Integer.valueOf(page_number), page);
        page = Vector.empty();
        ++page_number;
      }
      page = page.append(pair._2);
    }

    if (!page.isEmpty()) {
      result = result.put(Integer.valueOf(page_number), page);
    }

    return result;
  }
}
