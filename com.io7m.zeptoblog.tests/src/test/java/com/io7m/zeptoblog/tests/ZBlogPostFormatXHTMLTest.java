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

import com.io7m.zeptoblog.core.ZBlogPostFormatXHTML;
import com.io7m.zeptoblog.core.ZError;
import io.vavr.collection.Seq;
import io.vavr.control.Validation;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;

public final class ZBlogPostFormatXHTMLTest
{
  private static final Logger LOG;

  static {
    LOG = LoggerFactory.getLogger(ZBlogPostFormatXHTMLTest.class);
  }

  private static void dumpError(
    final Validation<Seq<ZError>, String> result)
  {
    if (result.isInvalid()) {
      result.getError().forEach(e -> LOG.error("{}: ", e, e.error().get()));
    }
  }

  @Test
  public void testPlain()
    throws Exception
  {
    final ZBlogPostFormatXHTML format = new ZBlogPostFormatXHTML();

    try (InputStream is =
           ZBlogPostFormatXHTMLTest.class.getResourceAsStream("simple.xhtml")) {
      final String text = IOUtils.toString(is, StandardCharsets.UTF_8);

      final Validation<Seq<ZError>, String> result =
        format.producePlain(Paths.get("/simple.xhtml"), text);

      dumpError(result);
      Assertions.assertTrue(result.isValid());

      System.out.println(result.get());
    }
  }
}
