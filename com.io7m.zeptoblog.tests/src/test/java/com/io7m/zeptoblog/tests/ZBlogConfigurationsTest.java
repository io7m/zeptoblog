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
import com.io7m.zeptoblog.core.ZBlogConfiguration;
import com.io7m.zeptoblog.core.ZBlogConfigurations;
import com.io7m.zeptoblog.core.ZError;
import javaslang.collection.Seq;
import javaslang.control.Validation;
import org.junit.Assert;
import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Properties;

public final class ZBlogConfigurationsTest
{
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
    final Properties p = this.baseProperties();
    p.put("com.io7m.zeptoblog.site_uri", " ");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertTrue(r.getError().get(0).error().get() instanceof URISyntaxException);
  }

  @Test
  public void testBadPostsPerPage()
  {
    final Properties p = this.baseProperties();
    p.put("com.io7m.zeptoblog.posts_per_page", "x");

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isInvalid());
    Assert.assertTrue(r.getError().get(0).error().get() instanceof JPropertyIncorrectType);
  }

  @Test
  public void testComplete()
  {
    final Properties p = this.baseProperties();

    final Validation<Seq<ZError>, ZBlogConfiguration> r =
      ZBlogConfigurations.fromProperties(Paths.get("/x/y/z"), p);
    Assert.assertTrue(r.isValid());

    final ZBlogConfiguration c = r.get();
    Assert.assertEquals("title", c.title());
    Assert.assertEquals(Paths.get("/source"), c.sourceRoot());
    Assert.assertEquals(Paths.get("/output"), c.outputRoot());
    Assert.assertEquals(URI.create("http://example.com"), c.siteURI());
    Assert.assertEquals("author", c.author());
    Assert.assertEquals(23L, (long) c.postsPerPage());
  }

  private Properties baseProperties()
  {
    final Properties p = new Properties();
    p.put("com.io7m.zeptoblog.title", "title");
    p.put("com.io7m.zeptoblog.source_root", "/source");
    p.put("com.io7m.zeptoblog.output_root", "/output");
    p.put("com.io7m.zeptoblog.site_uri", "http://example.com");
    p.put("com.io7m.zeptoblog.author", "author");
    p.put("com.io7m.zeptoblog.posts_per_page", "23");
    return p;
  }
}
