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

package com.io7m.zeptoblog.tests;

import com.io7m.zeptoblog.ZBlog;
import com.io7m.zeptoblog.ZBlogConfiguration;
import com.io7m.zeptoblog.ZBlogParserProviderType;
import com.io7m.zeptoblog.ZBlogParserType;
import com.io7m.zeptoblog.ZBlogPost;
import com.io7m.zeptoblog.ZError;
import javaslang.collection.Seq;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public abstract class ZBlogParserContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogParserContract.class);
  }

  protected abstract FileSystem createFilesystem();

  protected abstract ZBlogParserProviderType createParserProvider();

  @Test
  public void testNoSourceDirectory()
    throws Exception
  {
    final ZBlogParserProviderType prov = this.createParserProvider();

    try (final FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);

      final ZBlogParserType parser = prov.createParser(config);
      final Validation<Seq<ZError>, ZBlog> result = parser.parse();
      dumpResult(result);
      Assert.assertTrue(result.isInvalid());
      Assert.assertTrue(result.getError().get(0).error().get() instanceof NoSuchFileException);
    }
  }

  @Test
  public void testEmpty()
    throws Exception
  {
    final ZBlogParserProviderType prov = this.createParserProvider();

    try (final FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);
      Files.createDirectories(config.sourceRoot());

      final ZBlogParserType parser = prov.createParser(config);
      final Validation<Seq<ZError>, ZBlog> result = parser.parse();
      dumpResult(result);
      Assert.assertTrue(result.isValid());
      final ZBlog blog = result.get();
      Assert.assertTrue(blog.posts().isEmpty());
      Assert.assertTrue(blog.postsByDate().isEmpty());
      Assert.assertEquals("title", blog.title());
    }
  }

  @Test
  public void testOne()
    throws Exception
  {
    final ZBlogParserProviderType prov = this.createParserProvider();

    try (final FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);
      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (final BufferedWriter writer =
               Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
          writer.write("title Title");
          writer.newLine();
          writer.write("date 2020-01-01T00:00:00+0000");
          writer.newLine();
          writer.newLine();
          writer.write("Hello.");
          writer.newLine();
          writer.flush();
        }
      }

      final ZBlogParserType parser = prov.createParser(config);
      final Validation<Seq<ZError>, ZBlog> result = parser.parse();
      dumpResult(result);
      Assert.assertTrue(result.isValid());
      final ZBlog blog = result.get();
      Assert.assertEquals(1L, (long) blog.posts().size());
      Assert.assertEquals(1L, (long) blog.postsByDate().size());
      Assert.assertEquals("title", blog.title());

      {
        final ZBlogPost p = blog.posts().last()._2;
        Assert.assertEquals("Title", p.title());
        Assert.assertEquals(2020L, (long) p.date().getYear());
      }
    }
  }

  @Test
  public void testBrokenPost()
    throws Exception
  {
    final ZBlogParserProviderType prov = this.createParserProvider();

    try (final FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);
      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (final BufferedWriter writer =
               Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
          writer.write("nonsense");
          writer.newLine();
          writer.flush();
        }
      }

      final ZBlogParserType parser = prov.createParser(config);
      final Validation<Seq<ZError>, ZBlog> result = parser.parse();
      dumpResult(result);
      Assert.assertTrue(result.isInvalid());
    }
  }

  private static ZBlogConfiguration baseConfig(
    final FileSystem fs)
  {
    return ZBlogConfiguration.builder()
      .setOutputRoot(fs.getPath("output").toAbsolutePath())
      .setSourceRoot(fs.getPath("source").toAbsolutePath())
      .setTitle("title")
      .setPostsPerPage(10)
      .setIdLength(8)
      .setAuthor("author")
      .setSiteURI(URI.create("http://example.com"))
      .build();
  }

  private static <T> void  dumpResult(final Validation<Seq<ZError>, T> result)
  {
    if (result.isInvalid()) {
      result.getError().forEach(e -> LOG.error("{}", e));
    }
  }
}
