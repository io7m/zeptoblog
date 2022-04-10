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
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZBlogPost;
import com.io7m.zeptoblog.core.ZBlogPostParserProviderType;
import com.io7m.zeptoblog.core.ZBlogPostParserType;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public abstract class ZBlogPostParserContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogPostParserContract.class);
  }

  private static ByteArrayInputStream byteStream(
    final ByteArrayOutputStream os)
  {
    return new ByteArrayInputStream(os.toByteArray());
  }

  private static BufferedWriter writer(
    final ByteArrayOutputStream os)
  {
    return new BufferedWriter(new OutputStreamWriter(
      os,
      StandardCharsets.UTF_8));
  }

  private static <T> void dumpResult(final Validation<Seq<ZError>, T> result)
  {
    if (result.isInvalid()) {
      result.getError().forEach(e -> LOG.error("{}", e));
    }
  }

  protected abstract ZBlogPostParserProviderType createParserProvider();

  @Test
  public final void testUnexpectedEOF()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("");
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains(
          "Unexpected EOF"));
      }
    }
  }

  private ZBlogConfiguration config()
  {
    return ZBlogConfigurations.fromProperties(Paths.get(
      "/config.properties"), ZBlogConfigurationsTest.baseProperties()).get();
  }

  @Test
  public final void testBadDate0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("date");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains(
          "Syntax error"));
      }
    }
  }

  @Test
  public final void testBadDate1()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("date 2010");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains("2010"));
      }
    }
  }

  @Test
  public final void testBadTitle0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("title");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains(
          "Syntax error"));
      }
    }
  }

  @Test
  public final void testBadFormat0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("format");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains(
          "Syntax error"));
      }
    }
  }

  @Test
  public final void testBadNoTitle0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.newLine();
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isInvalid());
        Assertions.assertTrue(r.getError().get(0).message().contains(
          "Title not specified"));
      }
    }
  }

  @Test
  public final void testComplete()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("title A");
        w.newLine();
        w.write("date 2017-01-01T00:01:02+0000");
        w.newLine();
        w.write("format F");
        w.newLine();
        w.newLine();
        w.write("Hello.");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isValid());

        final ZBlogPost i = r.get();
        Assertions.assertEquals("A", i.title());
        Assertions.assertEquals("F", i.body().format());
        Assertions.assertTrue(i.body().text().contains("Hello."));
      }
    }
  }

  @Test
  public final void testCompleteWithoutFormat()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZBlogPostParserProviderType p_prov = this.createParserProvider();

    try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (BufferedWriter w = writer(os)) {
        w.write("title A");
        w.newLine();
        w.write("date 2017-01-01T00:01:02+0000");
        w.newLine();
        w.newLine();
        w.write("Hello.");
        w.newLine();
        w.flush();
      }

      try (ByteArrayInputStream is = byteStream(os)) {
        final ZBlogPostParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZBlogPost> r = p.parse();
        dumpResult(r);
        Assertions.assertTrue(r.isValid());

        final ZBlogPost i = r.get();
        Assertions.assertEquals("A", i.title());
        Assertions.assertEquals(
          ZBlogPostFormatCommonMark.NAME,
          i.body().format());
        Assertions.assertTrue(i.body().text().contains("Hello."));
      }
    }
  }
}
