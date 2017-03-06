zeptoblog
===

[![Build Status](https://travis-ci.org/io7m/zeptoblog.svg)](https://travis-ci.org/io7m/zeptoblog)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.io7m.zeptoblog/com.io7m.zeptoblog/badge.png)](https://maven-badges.herokuapp.com/maven-central/com.io7m.zeptoblog/com.io7m.zeptoblog)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6589f45ce9894044b13940a85aaf555c)](https://www.codacy.com/app/github_79/zeptoblog?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=io7m/zeptoblog&amp;utm_campaign=Badge_Grade)

![zeptoblog](./src/site/resources/zeptoblog.jpg?raw=true)

## Contents

* [Usage](#usage)
* [Page Syntax](#page-syntax)
* [Supported Formats](#supported-formats)
* [Real-world Examples](#real-world-examples)

## Usage

Create a configuration file (in [Java properties](https://docs.oracle.com/javase/8/docs/api/java/util/Properties.html) format):

```
# The blog title
com.io7m.zeptoblog.title = Example Blog

# The blog source tree
com.io7m.zeptoblog.source_root = /home/someone/blog-src

# The output directory tree
com.io7m.zeptoblog.output_root = /tmp/blog-out

# The number of posts per page
com.io7m.zeptoblog.posts_per_page = 30

# The site URI
com.io7m.zeptoblog.site_uri = http://blog.io7m.com/

# The author information that will appear in Atom feeds
com.io7m.zeptoblog.author = blog@io7m.com

# The default format of blog posts (CommonMark, here)
com.io7m.zeptoblog.format_default = com.io7m.zeptoblog.commonmark
```

Create posts by creating files in `com.io7m.zeptoblog.source_root` with names ending in `.zbp`.

Posts must consist of a series of commands that specify the date
(in [ISO 8601](https://en.wikipedia.org/wiki/ISO_8601) format) and
title of the post, followed by an empty line, followed by the body
of the post in [CommonMark](http://commonmark.org/) format.

```
$ cat /home/someone/blog-src/2017/02/24/post.zbp
title An example post
date 2017-02-24T19:37:48+0000

Lorem ipsum dolor sit amet, consectetur adipiscing elit. Curabitur
efficitur sed nisi ac volutpat.

![Ladybug](/2017/02/24/ladybug.jpg)
```

Files can appear anywhere in `com.io7m.zeptoblog.source_root`,
including any subdirectory, and directory names do not carry any
specific meaning. Organizing posts by `year/month/day` is merely a
useful convention. Any files with names not ending in `.zbp` will
be copied unmodified to the output directory.

Compile the blog:

```
$ java -jar com.io7m.zeptoblog.cmdline-0.3.0-main.jar compile -config blog.conf
```

Sign pages with `gpg`:

```
$ find /tmp/blog-out -name '*.xhtml' -type f -exec gpg -a --detach-sign -u 'my key id' {} \;
```

Use [rsync](https://rsync.samba.org/) to copy `/tmp/blog-out` to a site.

## Page Syntax

A `zbp` file has the following syntax (given in [EBNF](https://en.wikipedia.org/wiki/Extended_Backus-Naur_form)):

```
text =
  ? any unicode text not including newlines ? ;

newline =
  U+000A | U+00OD , U+000A ;

title_command =
  "title" , { text } , newline ;

date_command =
  "date" , timestamp , newline ;

format_name =
  { (? any unicode letter and number ?) | "." } ;

format_command =
  "format" , format_name , newline ;

header =
  { title_command | date_command | format_command } ;

body =
  { text } ;

zbp =
  header , newline , body ;
```

The `title` command sets the title for a post.
The `date` command sets the date for a post. If no date is specified, then the post will not
appear in any listings of posts such as the generated "posts by year" page.
The `format` command specifies the [format](#supported-formats) of the body of the post.

```
title An XHTML post
date 2017-03-06T13:25:16+0000
format com.io7m.zeptoblog.xhtml

<div>
  <p>An XHTML post.</p>
</div>
```

## Supported Formats

By default, the `zeptoblog` distribution supports the following formats:

| Format                        | Description                          |
| ----------------------------- | ------------------------------------ |
| com.io7m.zeptoblog.commonmark | [CommonMark](https://commonmark.org) |
| com.io7m.zeptoblog.xhtml      | XHTML                                |

Additional formats can be implemented by implementing the [ZBlogPostFormatType](https://github.com/io7m/zeptoblog/blob/develop/com.io7m.zeptoblog.core/src/main/java/com/io7m/zeptoblog/core/ZBlogPostFormatType.java)
and registering the implementation as a [service provider](https://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).

The detected formats can be listed from the command line:

```
$ java -jar com.io7m.zeptoblog.cmdline-0.3.0-main.jar formats
com.io7m.zeptoblog.xhtml         : XHTML 1.0 Strict
com.io7m.zeptoblog.commonmark    : http://commonmark.org 0.27
```

## Real-world Examples

http://blog.io7m.com
