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

import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.zeptoblog.commonmark.ZBlogPostFormatCommonMark;
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZBlogPostGeneratorRequest;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.hamcrest.core.StringContains;
import org.junit.Assert;
import org.junit.Test;

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
    Assert.assertTrue(r.isInvalid());
  }

  @Test
  public void testBadURI()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.site_uri", " ");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertTrue(r.getError().get(0).error().get() instanceof URISyntaxException);
  }

  @Test
  public void testBadPostsPerPage()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.posts_per_page", "x");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertTrue(r.getError().get(0).error().get() instanceof JPropertyIncorrectType);
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
    Assert.assertTrue(r.isValid());

    final ZBlogConfiguration c = r.get();
    Assert.assertEquals("title", c.title());
    Assert.assertEquals(Paths.get("/source").toAbsolutePath(), c.sourceRoot());
    Assert.assertEquals(Paths.get("/output").toAbsolutePath(), c.outputRoot());
    Assert.assertEquals(URI.create("http://example.com"), c.siteURI());
    Assert.assertEquals("author", c.author());
    Assert.assertEquals(23L, (long) c.postsPerPage());

    final ZBlogPostGeneratorRequest g = c.generatorRequests().get("x").get();
    Assert.assertEquals(g.name(), "x");
    Assert.assertEquals(g.generatorName(), "y");
    Assert.assertEquals(g.configFile().toString(), "z");
  }

  @Test
  public void testBadGeneratorRequestMissing()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("x"));
  }

  @Test
  public void testBadGeneratorRequestMissingFile()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");
    p.put("com.io7m.zeptoblog.generators.x.type", "y");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("file"));
  }

  @Test
  public void testBadGeneratorRequestMissingType()
  {
    final Properties p = baseProperties();
    p.put("com.io7m.zeptoblog.generators", "x");
    p.put("com.io7m.zeptoblog.generators.x.file", "y");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertThat(
      r.getError().get(0).message(),
      StringContains.containsString("type"));
  }
}
