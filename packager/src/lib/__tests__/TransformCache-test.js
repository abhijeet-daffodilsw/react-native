/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

'use strict';

jest
  .dontMock('json-stable-stringify')
  .dontMock('../TransformCache')
  .dontMock('left-pad')
  .dontMock('lodash/throttle')
  .dontMock('crypto');

const crypto = require('crypto');
const jsonStableStringify = require('json-stable-stringify');

const mockFS = new Map();

jest.mock('fs', () => ({
  readFileSync(filePath) {
    return mockFS.get(filePath);
  },
  unlinkSync(filePath) {
    mockFS.delete(filePath);
  },
  readdirSync(dirPath) {
    // Not required for it to work.
    return [];
  },
}));

jest.mock('write-file-atomic', () => ({
  sync(filePath, data) {
    mockFS.set(filePath, data.toString());
  },
}));

jest.mock('rimraf', () => () => {});

function cartesianProductOf(a1, a2) {
  const product = [];
  a1.forEach(e1 => a2.forEach(e2 => product.push([e1, e2])));
  return product;
}

describe('TransformCache', () => {

  let TransformCache;

  beforeEach(() => {
    jest.resetModules();
    mockFS.clear();
    TransformCache = require('../TransformCache');
  });

  it('is caching different files and options separately', () => {
    const argsFor = ([filePath, transformOptions]) => {
      const key = filePath + JSON.stringify(transformOptions);
      return {
        sourceCode: `/* source for ${key} */`,
        getTransformCacheKey: () => 'abcdef',
        filePath,
        transformOptions,
        transformOptionsKey: crypto.createHash('md5')
          .update(jsonStableStringify(transformOptions)).digest('hex'),
        result: {
          code: `/* result for ${key} */`,
          dependencies: ['foo', `dep of ${key}`],
          dependencyOffsets: [12, 34],
          map: {desc: `source map for ${key}`},
        },
      };
    };
    const allCases = cartesianProductOf(
      ['/some/project/sub/dir/file.js', '/some/project/other.js'],
      [{foo: 1}, {foo: 2}],
    );
    allCases.forEach(
      entry => TransformCache.writeSync(argsFor(entry)),
    );
    allCases.forEach(entry => {
      const args = argsFor(entry);
      const {result} = args;
      const cachedResult = TransformCache.readSync({
        ...args,
        cacheOptions: {resetCache: false},
      });
      expect(cachedResult.result).toEqual(result);
    });
  });

  it('is overriding cache when source code or transform key changes', () => {
    const argsFor = ([sourceCode, transformCacheKey]) => {
      const key = sourceCode + transformCacheKey;
      return {
        sourceCode,
        getTransformCacheKey: () => transformCacheKey,
        filePath: 'test.js',
        transformOptions: {foo: 1},
        transformOptionsKey: 'boo!',
        result: {
          code: `/* result for ${key} */`,
          dependencies: ['foo', 'bar'],
          dependencyOffsets: [12, 34],
          map: {desc: `source map for ${key}`},
        },
      };
    };
    const allCases = cartesianProductOf(
      ['/* foo */', '/* bar */'],
      ['abcd', 'efgh'],
    );
    allCases.forEach(entry => {
      TransformCache.writeSync(argsFor(entry));
      const args = argsFor(entry);
      const {result} = args;
      const cachedResult = TransformCache.readSync({
        ...args,
        cacheOptions: {resetCache: false},
      });
      expect(cachedResult.result).toEqual(result);
    });
    allCases.pop();
    allCases.forEach(entry => {
      const cachedResult = TransformCache.readSync({
        ...argsFor(entry),
        cacheOptions: {resetCache: false},
      });
      expect(cachedResult.result).toBeNull();
      expect(cachedResult.outdatedDependencies).toEqual(['foo', 'bar']);
    });
  });

});
