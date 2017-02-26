zeptoblog
===

[![Build Status](https://travis-ci.org/io7m/zeptoblog.svg)](https://travis-ci.org/io7m/zeptoblog)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/6589f45ce9894044b13940a85aaf555c)](https://www.codacy.com/app/github_79/zeptoblog?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=io7m/zeptoblog&amp;utm_campaign=Badge_Grade)

![zeptoblog](./src/site/resources/zeptoblog.jpg?raw=true)

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
```

Create posts by creating files in `com.io7m.zeptoblog.source_root` with names ending in `.zbp`:

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
$ java -jar com.io7m.zeptoblog-0.2.0-main.jar compile -config blog.conf
```

Sign pages with `gpg`:

```
$ find /tmp/blog-out -name '*.xhtml' -type f -exec gpg -a --detach-sign -u 'my key id' {} \;
```

Use [rsync](https://rsync.samba.org/) to copy `/tmp/blog-out` to a site.

