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

import com.io7m.zeptoblog.ZBlogParserProvider;
import com.io7m.zeptoblog.ZBlogParserProviderType;
import com.io7m.zeptoblog.ZBlogPostParserProvider;
import com.io7m.zeptoblog.ZBlogWriterProvider;
import com.io7m.zeptoblog.ZBlogWriterProviderType;

import java.nio.file.FileSystem;

public final class ZBlogWriterDOSTest extends ZBlogWriterContract
{
  @Override
  protected FileSystem createFilesystem()
  {
    return TestFilesystems.makeEmptyDOSFilesystem();
  }

  @Override
  protected ZBlogParserProviderType createParserProvider()
  {
    final ZBlogParserProvider provider = new ZBlogParserProvider();
    provider.setBlogPostParserProvider(new ZBlogPostParserProvider());
    return provider;
  }

  @Override
  protected ZBlogWriterProviderType createWriterProvider()
  {
    return new ZBlogWriterProvider();
  }
}
