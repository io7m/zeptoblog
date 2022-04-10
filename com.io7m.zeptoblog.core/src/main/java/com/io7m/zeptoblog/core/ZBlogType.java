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

import io.vavr.Tuple2;
import io.vavr.collection.Iterator;
import io.vavr.collection.Seq;
import io.vavr.collection.SortedMap;
import io.vavr.collection.TreeMap;
import io.vavr.collection.Vector;
import org.immutables.value.Value;
import org.immutables.vavr.encodings.VavrEncodingEnabled;

import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.function.Function;

/**
 * The type of blogs.
 */

@ZImmutableStyleType
@VavrEncodingEnabled
@Value.Immutable
public interface ZBlogType
{
  /**
   * @return The blog title
   */

  @Value.Parameter
  String title();

  /**
   * @return The blog posts by path
   */

  @Value.Parameter
  SortedMap<Path, ZBlogPost> posts();

  /**
   * @return The blog posts by date
   */

  @Value.Lazy
  default SortedMap<ZonedDateTime, ZBlogPost> postsByDate()
  {
    SortedMap<ZonedDateTime, ZBlogPost> results = TreeMap.empty();
    for (final ZBlogPost post : this.posts().values()) {
      final Optional<ZonedDateTime> date_opt = post.date();
      if (date_opt.isPresent()) {
        results = results.put(date_opt.get(), post);
      }
    }

    return results;
  }

  /**
   * Group the blog posts by page.
   *
   * @param count The number of posts per page
   *
   * @return A list of pages containing posts
   */

  default SortedMap<Integer, Seq<ZBlogPost>> postsGroupedByPage(
    final int count)
  {
    SortedMap<Integer, Seq<ZBlogPost>> result = TreeMap.empty();

    int page_number = 0;
    Vector<ZBlogPost> page = Vector.empty();

    final Iterator<Tuple2<ZonedDateTime, ZBlogPost>> iterator =
      this.postsByDate().map(Function.identity()).reverseIterator();

    while (iterator.hasNext()) {
      final Tuple2<ZonedDateTime, ZBlogPost> pair = iterator.next();
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

  /**
   * Group the blog posts by year.
   *
   * @return A list of pages containing posts
   */

  @Value.Lazy
  default SortedMap<Integer, Seq<ZBlogPost>> postsGroupedByYear()
  {
    SortedMap<Integer, Seq<ZBlogPost>> result = TreeMap.empty();

    final Iterator<Tuple2<ZonedDateTime, ZBlogPost>> iterator =
      this.postsByDate().map(Function.identity()).reverseIterator();

    while (iterator.hasNext()) {
      final Tuple2<ZonedDateTime, ZBlogPost> pair = iterator.next();
      final ZonedDateTime date = pair._1;

      Seq<ZBlogPost> year_seq;
      final Integer year = Integer.valueOf(date.getYear());
      if (result.containsKey(year)) {
        year_seq = result.get(year).get();
      } else {
        year_seq = Vector.empty();
      }

      year_seq = year_seq.append(pair._2);
      result = result.put(year, year_seq);
    }

    return result;
  }
}
