package cfe.support;
/*
 * Extremely C language Compiler.
 * Copyright (c) 2015-2020, Jianping Zeng.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied.  See the License for the specific language governing
 * permissions and limitations under the License.
 */

/**
 * <p>
 * Information about a FileID, basically just the logical file
 * that it represents and include stack information.
 * </p>
 * <p>
 * Each FileInfo has include stack information, indicating where it came
 * from.  This information encodes the #include chain that a token was
 * instantiated from.  The main include file has an invalid IncludeLoc.
 * </p>
 * FileInfos contain a "ContentCache " reference, with the contents of the file.
 *
 * @author Jianping Zeng
 * @version 0.4
 */
public class FileInfo {
  private int includeLoc;

  private ContentCache data;

  /**
   * if this FileID has #line directives in it.
   */
  private boolean lineDirectives;

  private CharacteristicKind characteristic;

  public static FileInfo get(SourceLocation loc, ContentCache con,
                             CharacteristicKind fileCharacter) {
    FileInfo info = new FileInfo();
    info.includeLoc = loc.getRawEncoding();
    info.data = con;
    info.characteristic = fileCharacter;
    return info;
  }

  public boolean hasLineDirectives() {
    return lineDirectives;
  }

  public void setLineDirectives(boolean lineDirectives) {
    this.lineDirectives = lineDirectives;
  }

  public SourceLocation getIncludeLoc() {
    return SourceLocation.getFromRawEncoding(includeLoc);
  }

  public ContentCache getContentCache() {
    return data;
  }

  public CharacteristicKind getFileCharacteristic() {
    return characteristic;
  }
}
