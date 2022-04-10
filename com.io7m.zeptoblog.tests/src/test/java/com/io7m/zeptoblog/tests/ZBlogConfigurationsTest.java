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

import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.zeptoblog.commonmark.ZBlogPostFormatCommonMark;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorRequest;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;

public final class ZBlogConfigurationsTest
{
  public static Properties baseProperties()
  {
    final Properties p = new Properties();
    p.put("com.io7m.zeptoblog.title", "title");
    p.put("com.io7m.zeptoblog.source_root", "/source");
    p.put("com.io7m.zeptoblog.output_root", "/output");
    p.put("com.io7m.zeptoblog.site_uri", "http://example.com");
    p.put("com.io7m.zeptoblog.author", "author");
    p.put("com.io7m.zeptoblog.posts_per_page", "23");
    p.put("com.io7m.zeptoblog.format_default", ZBlogPostFormatCommonMark.NAME);
    return p;
  }

  @Test
  public void testEmpty()
  {
    final Properties p = new Properties();
    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
  }

  @Test
  public void testBadURI()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.site_uri", " ");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
    Assertions.assertTrue(r.getError().get(0).error().get() instanceof URISyntaxException);
  }

  @Test
  public void testBadPostsPerPage()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.posts_per_page", "x");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
    Assertions.assertTrue(r.getError().get(0).error().get() instanceof JPropertyIncorrectType);
  }

  @Test
  public void testComplete()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");
    p.put("com.io7m.zeptoblog.generators.x.type", "y");
    p.put("com.io7m.zeptoblog.generators.x.file", "z");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isValid());

    final ZBlogConfiguration c = r.get();
    Assertions.assertEquals("title", c.title());
    Assertions.assertEquals(
      Paths.get("/source").toAbsolutePath(),
      c.sourceRoot());
    Assertions.assertEquals(
      Paths.get("/output").toAbsolutePath(),
      c.outputRoot());
    Assertions.assertEquals(URI.create("http://example.com"), c.siteURI());
    Assertions.assertEquals("author", c.author());
    Assertions.assertEquals(23L, c.postsPerPage());

    final ZBlogPostGeneratorRequest g = c.generatorRequests().get("x").get();
    Assertions.assertEquals(g.name(), "x");
    Assertions.assertEquals(g.generatorName(), "y");
    Assertions.assertEquals(g.configFile().toString(), "z");
  }

  @Test
  public void testBadGeneratorRequestMissing()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
    Assertions.assertTrue(r.getError().get(0).message().contains("x"));
  }

  @Test
  public void testBadGeneratorRequestMissingFile()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");
    p.put("com.io7m.zeptoblog.generators.x.type", "y");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
    Assertions.assertTrue(r.getError().get(0).message().contains("file"));
  }

  @Test
  public void testBadGeneratorRequestMissingType()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");
    p.put("com.io7m.zeptoblog.generators.x.file", "y");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assertions.assertTrue(r.isInvalid());
    Assertions.assertTrue(r.getError().get(0).message().contains("type"));
  }
}
