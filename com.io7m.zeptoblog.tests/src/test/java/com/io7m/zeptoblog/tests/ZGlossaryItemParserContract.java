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

import com.io7m.zeptoblog.commonmark.ZBlogPostFormatCommonMark;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZError;
import com.io7m.zeptoblog.glossary.ZGlossaryItem;
import com.io7m.zeptoblog.glossary.ZGlossaryItemParserProviderType;
import com.io7m.zeptoblog.glossary.ZGlossaryItemParserType;
import javaslang.collection.HashSet;
import javaslang.collection.Seq;
import javaslang.control.Validation;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public abstract class ZGlossaryItemParserContract
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZGlossaryItemParserContract.class);
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

  protected abstract ZGlossaryItemParserProviderType createParserProvider();

  private ZBlogConfiguration config()
  {
    return ZBlogConfigurations.fromProperties(Paths.get(
      "/config.properties"), ZBlogConfigurationsTest.baseProperties()).get();
  }

  @Test
  public final void testUnexpectedEOF()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("");
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isInvalid());
        Assert.assertTrue(r.getError().get(0).message().contains(
          "Unexpected EOF"));
      }
    }
  }

  @Test
  public final void testBadTerm0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("term");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isInvalid());
        Assert.assertTrue(r.getError().get(0).message().contains("Syntax error"));
      }
    }
  }

  @Test
  public final void testBadFormat0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("format");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isInvalid());
        Assert.assertTrue(r.getError().get(0).message().contains("Syntax error"));
      }
    }
  }

  @Test
  public final void testBadRelated0()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("related");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isInvalid());
        Assert.assertTrue(r.getError().get(0).message().contains("Syntax error"));
      }
    }
  }

  @Test
  public final void testUnrecognized()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("unknown");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isInvalid());
        Assert.assertTrue(r.getError().get(0).message().contains("Unrecognized"));
      }
    }
  }

  @Test
  public final void testComplete()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("term A");
        w.newLine();
        w.write("related X Y Z");
        w.newLine();
        w.write("format F");
        w.newLine();
        w.newLine();
        w.write("Hello.");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isValid());

        final ZGlossaryItem i = r.get();
        Assert.assertEquals("A", i.term());
        Assert.assertEquals(HashSet.of("X", "Y", "Z"), i.seeAlso());
        Assert.assertEquals("F", i.body().format());
        Assert.assertThat(i.body().text(), StringContains.containsString("Hello."));
      }
    }
  }

  @Test
  public final void testCompleteWithoutFormat()
    throws Exception
  {
    final ZBlogConfiguration config = this.config();

    final ZGlossaryItemParserProviderType p_prov = this.createParserProvider();

    try (final ByteArrayOutputStream os = new ByteArrayOutputStream()) {
      try (final BufferedWriter w = writer(os)) {
        w.write("term A");
        w.newLine();
        w.write("related X Y Z");
        w.newLine();
        w.newLine();
        w.write("Hello.");
        w.newLine();
        w.flush();
      }

      try (final ByteArrayInputStream is = byteStream(os)) {
        final ZGlossaryItemParserType p =
          p_prov.createParser(config, is, Paths.get("/x/y/z"));

        final Validation<Seq<ZError>, ZGlossaryItem> r = p.parse();
        dumpResult(r);
        Assert.assertTrue(r.isValid());

        final ZGlossaryItem i = r.get();
        Assert.assertEquals("A", i.term());
        Assert.assertEquals(HashSet.of("X", "Y", "Z"), i.seeAlso());
        Assert.assertEquals(ZBlogPostFormatCommonMark.NAME, i.body().format());
        Assert.assertThat(i.body().text(), StringContains.containsString("Hello."));
      }
    }
  }
}
