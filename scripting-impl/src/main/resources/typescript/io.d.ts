
declare type FilePath = java.nio.file.Path | string

/**
 * Gets a filepath to a file described by a string.
 * 
 * If the path starts with a '.' the path will be relative to the script's
 * own file.
 * 
 * Otherwise the path is relative to the server directory itself
 * 
 * If a path starts with "../" it's treated as going 1 level up from the
 * script's own path
 * 
 * @param path String path.
 */
declare function getPath(path: string): java.nio.file.Path

declare function fileExists(path: FilePath): boolean

declare function deleteFile(path: FilePath): void

/**
 * Reads a file as text
 * @param path File path, @see getPath for semantic information
 */
declare function readText(path: FilePath): string

declare function readLines(path: FilePath): string[]

declare function readJson(path: FilePath): any


declare function writeText(path: FilePath, content: string | string[]): void

declare function writeJson(path: FilePath, content: any): void