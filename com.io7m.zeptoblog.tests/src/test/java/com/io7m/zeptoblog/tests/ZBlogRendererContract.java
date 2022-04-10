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
import com.io7m.zeptoblog.core.ZBlog;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogParserProviderType;
import com.io7m.zeptoblog.core.ZBlogParserType;
import com.io7m.zeptoblog.core.ZBlogRendererProviderType;
import com.io7m.zeptoblog.core.ZBlogRendererType;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class ZBlogRendererContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogRendererContract.class);
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

  private static void runOne(
    final ZBlogParserProviderType p_prov,
    final ZBlogRendererProviderType w_prov,
    final ZBlogConfiguration config)
  {
    final ZBlogParserType parser = p_prov.createParser(config);
    final Validation<Seq<ZError>, ZBlog> result = parser.parse();
    dumpResult(result);
    Assertions.assertTrue(result.isValid());
    final ZBlog blog = result.get();

    final ZBlogRendererType writer = w_prov.createRenderer(config);
    final Validation<Seq<ZError>, Void> w_result = writer.render(blog);
    dumpResult(w_result);
    Assertions.assertTrue(w_result.isValid());

    Assertions.assertTrue(
      Files.isRegularFile(config.outputRoot().resolve("one.xhtml")));
    Assertions.assertTrue(
      Files.isRegularFile(config.outputRoot().resolve("1.xhtml")));
    Assertions.assertTrue(
      Files.isRegularFile(config.outputRoot().resolve("yearly.xhtml")));
    Assertions.assertTrue(
      Files.isRegularFile(config.outputRoot().resolve("blog.atom")));
  }

  protected abstract FileSystem createFilesystem();

  protected abstract ZBlogParserProviderType createParserProvider();

  protected abstract ZBlogRendererProviderType createWriterProvider();

  @Test
  public final void testEmpty()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);
      Files.createDirectories(config.sourceRoot());
      Files.createDirectories(config.outputRoot());

      final ZBlogParserType parser = p_prov.createParser(config);
      final Validation<Seq<ZError>, ZBlog> p_result = parser.parse();
      dumpResult(p_result);
      Assertions.assertTrue(p_result.isValid());
      final ZBlog blog = p_result.get();

      final ZBlogRendererType writer = w_prov.createRenderer(config);
      final Validation<Seq<ZError>, Void> w_result = writer.render(blog);
      dumpResult(w_result);
      Assertions.assertTrue(w_result.isValid());
    }
  }

  @Test
  public final void testOne()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final ZBlogConfiguration config = baseConfig(fs);
      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }

  @Test
  public final void testFooterPre()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final Path mod_path = fs.getPath("insert.xml");
      Files.copy(
        ZBlogRendererContract.class.getResourceAsStream(
          "/com/io7m/zeptoblog/tests/insertable.xml"),
        mod_path);

      final ZBlogConfiguration config =
        ZBlogConfiguration.builder()
          .from(baseConfig(fs))
          .setFooterPre(mod_path)
          .build();

      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }

  @Test
  public final void testFooterPost()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final Path mod_path = fs.getPath("insert.xml");
      Files.copy(
        ZBlogRendererContract.class.getResourceAsStream(
          "/com/io7m/zeptoblog/tests/insertable.xml"),
        mod_path);

      final ZBlogConfiguration config =
        ZBlogConfiguration.builder()
          .from(baseConfig(fs))
          .setFooterPost(mod_path)
          .build();

      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }

  @Test
  public final void testHeaderPre()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final Path mod_path = fs.getPath("insert.xml");
      Files.copy(
        ZBlogRendererContract.class.getResourceAsStream(
          "/com/io7m/zeptoblog/tests/insertable.xml"),
        mod_path);

      final ZBlogConfiguration config =
        ZBlogConfiguration.builder()
          .from(baseConfig(fs))
          .setHeaderPre(mod_path)
          .build();

      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }

  @Test
  public final void testHeaderPost()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final Path mod_path = fs.getPath("insert.xml");
      Files.copy(
        ZBlogRendererContract.class.getResourceAsStream(
          "/com/io7m/zeptoblog/tests/insertable.xml"),
        mod_path);

      final ZBlogConfiguration config =
        ZBlogConfiguration.builder()
          .from(baseConfig(fs))
          .setHeaderPost(mod_path)
          .build();

      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }

  @Test
  public final void testHeaderReplace()
    throws Exception
  {
    final ZBlogParserProviderType p_prov = this.createParserProvider();
    final ZBlogRendererProviderType w_prov = this.createWriterProvider();

    try (FileSystem fs = this.createFilesystem()) {
      final Path mod_path = fs.getPath("insert.xml");
      Files.copy(
        ZBlogRendererContract.class.getResourceAsStream(
          "/com/io7m/zeptoblog/tests/insertable.xml"),
        mod_path);

      final ZBlogConfiguration config =
        ZBlogConfiguration.builder()
          .from(baseConfig(fs))
          .setHeaderReplace(mod_path)
          .build();

      Files.createDirectories(config.sourceRoot());

      {
        final Path file = config.sourceRoot().resolve("one.zbp");
        try (BufferedWriter writer =
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

      runOne(p_prov, w_prov, config);
    }
  }
}
