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

package com.io7m.zeptoblog.tests;

import com.io7m.zeptoblog.commonmark.ZBlogPostFormatCommonMark;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogPost;
import com.io7m.zeptoblog.core.ZBlogPostBody;
import com.io7m.zeptoblog.core.ZBlogPostParserProviderType;
import com.io7m.zeptoblog.core.ZBlogPostParserType;
import com.io7m.zeptoblog.core.ZBlogPostSerializerType;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

public abstract class ZBlogPostSerializerContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogPostSerializerContract.class);
  }

  private static ZBlogConfiguration baseConfig(
    final FileSystem fs)
  {
    return ZBlogConfiguration.builder()
      .setOutputRoot(fs.getPath("output").toAbsolutePath())
      .setSourceRoot(fs.getPath("source").toAbsolutePath())
      .setTitle("title")
      .setPostsPerPage(10)
      .setAuthor("author")
      .setFormatDefault(ZBlogPostFormatCommonMark.NAME)
      .setSiteURI(URI.create("http://example.com"))
      .build();
  }

  private static <T> void dumpResult(final Validation<Seq<ZError>, T> result)
  {
    if (result.isInvalid()) {
      result.getError().forEach(e -> LOG.error("{}", e));
    }
  }

  protected abstract FileSystem createFilesystem();

  protected abstract ZBlogPostParserProviderType createParserProvider();

  protected abstract ZBlogPostSerializerType createSerializer();

  @Test
  public void testSerializeRoundTrip()
    throws Exception
  {
    final ZBlogPostParserProviderType prov = this.createParserProvider();
    final ZBlogPostSerializerType serial = this.createSerializer();

    try (FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);

      Files.createDirectories(config.sourceRoot());

      final Path post_path =
        config.sourceRoot().resolve("post.zbp");

      final ZBlogPost post =
        ZBlogPost.builder()
          .setPath(post_path)
          .setTitle("Title")
          .setDate(Optional.of(ZonedDateTime.of(
            2001,
            1,
            2,
            3,
            4,
            5,
            0,
            ZoneId.of("Z"))))
          .setBody(ZBlogPostBody.of(
            ZBlogPostFormatCommonMark.NAME,
            "Content." + System.lineSeparator()))
          .build();

      final Validation<Seq<ZError>, String> serial_result =
        serial.serialize(post);
      dumpResult(serial_result);
      final String out_text = serial_result.get();
      LOG.debug(out_text);
      Files.write(post_path, out_text.getBytes(StandardCharsets.UTF_8));

      try (InputStream stream = Files.newInputStream(post_path)) {
        final ZBlogPostParserType parser =
          prov.createParser(config, stream, post_path);
        final Validation<Seq<ZError>, ZBlogPost> result = parser.parse();
        dumpResult(result);

        final ZBlogPost parsed = result.get();
        Assertions.assertEquals(post.title(), parsed.title());
        Assertions.assertEquals(post.body(), parsed.body());
        Assertions.assertEquals(post.date(), parsed.date());
        Assertions.assertEquals(post.path(), parsed.path());
      }
    }
  }
}
