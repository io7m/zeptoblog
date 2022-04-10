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

import com.io7m.zeptoblog.glossary.ZGlossaryGenerator;
import com.io7m.zeptoblog.glossary.ZGlossaryItemParserProvider;
import com.io7m.zeptoblog.glossary.ZGlossaryItemParserProviderType;
import com.io7m.zeptoblog.glossary.ZGlossaryParserProvider;
import com.io7m.zeptoblog.glossary.ZGlossaryParserProviderType;

/**
 * Static blog generator (Glossary generation)
 */

module com.io7m.zeptoblog.glossary
{
  requires static org.immutables.value;
  requires static org.immutables.vavr.encodings;
  requires static org.osgi.annotation.bundle;
  requires static org.osgi.annotation.versioning;
  requires static org.osgi.service.component.annotations;

  requires com.io7m.jlexing.core;
  requires com.io7m.jproperties.core;
  requires com.io7m.zeptoblog.core;
  requires io.vavr;
  requires java.xml;
  requires org.apache.commons.io;
  requires org.slf4j;

  provides com.io7m.zeptoblog.core.ZBlogPostGeneratorType
    with ZGlossaryGenerator;
  provides ZGlossaryItemParserProviderType
    with ZGlossaryItemParserProvider;
  provides ZGlossaryParserProviderType
    with ZGlossaryParserProvider;

  exports com.io7m.zeptoblog.glossary;
}
