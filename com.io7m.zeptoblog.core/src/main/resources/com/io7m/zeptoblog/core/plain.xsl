<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet
  xmlns:xh="http://www.w3.org/1999/xhtml"
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="2.0">
  <xsl:output method="text" indent="no"/>

  <xsl:variable name="new-line" select="'&#10;'"/>

  <xsl:variable name="new-lines" select="concat( $new-line, $new-line )"/>

  <xsl:template match="*">
    <xsl:apply-templates select="text()|*"/>
  </xsl:template>

  <xsl:template match="text()">
    <xsl:value-of select="normalize-space(.)"/><xsl:text> </xsl:text>
  </xsl:template>

  <xsl:template match="xh:h1|xh:h2|xh:h3|xh:h4|xh:h5">
    <xsl:value-of select="normalize-space(.)"/>
    <xsl:value-of select="$new-line"/>
    <xsl:text>====</xsl:text>
    <xsl:value-of select="$new-lines"/>
  </xsl:template>

  <xsl:template match="xh:p|xh:blockquote|xh:li|xh:div">
    <xsl:apply-templates select="text()|*"/>
    <xsl:value-of select="$new-lines"/>
  </xsl:template>

  <xsl:template match="xh:table">
    <xsl:apply-templates select="*"/>
    <xsl:value-of select="$new-line"/>
  </xsl:template>

  <xsl:template match="xh:tr">
    <xsl:apply-templates select="*"/>
    <xsl:value-of select="$new-line"/>
  </xsl:template>

  <xsl:template match="xh:td">
    <xsl:value-of select="'[ '"/>
    <xsl:apply-templates select="text()|*"/>
    <xsl:value-of select="' ]'"/>
  </xsl:template>

  <xsl:template match="xh:a">
    <xsl:text>[</xsl:text>
    <xsl:value-of select="text()"/>
    <xsl:text> (</xsl:text>
    <xsl:value-of select="@href"/>
    <xsl:text>)]</xsl:text>
  </xsl:template>

  <xsl:template match="xh:b|xh:strong|xh:em|xh:span|xh:strike">
    <xsl:value-of select="normalize-space(.)"/>
  </xsl:template>

  <xsl:template match="xh:img">
    <xsl:text>[image: </xsl:text>
    <xsl:value-of select="@src"/>
    <xsl:text> </xsl:text>
    <xsl:value-of select="@alt"/>
    <xsl:text>]</xsl:text>
    <xsl:value-of select="$new-lines"/>
  </xsl:template>

  <xsl:template match="xh:hr" name="hr">
    <xsl:text>----</xsl:text>
    <xsl:value-of select="$new-lines"/>
  </xsl:template>

  <xsl:template match="xh:br">
    <xsl:value-of select="$new-line"/>
  </xsl:template>

</xsl:stylesheet>