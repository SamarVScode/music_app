/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop â€” Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
/*
   Copyright 2025 Kyant

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package org.oxycblt.auxio.ui.ios.backdrop.internal

import org.intellij.lang.annotations.Language

@Language("AGSL")
private const val RoundedRectSDF = """
half radiusAt(float2 coord, half4 radii) {
    half top = mix(radii.x, radii.y, step(0.0, coord.x));
    half bottom = mix(radii.w, radii.z, step(0.0, coord.x));
    return mix(top, bottom, step(0.0, coord.y));
}

half sdRoundedRect(float2 coord, half2 halfSize, half radius) {
    half2 p = abs(half2(coord));
    half2 b = halfSize;
    half2 d = p - b + radius;
    return length(max(d, half2(0.0))) + min(max(d.x, d.y), 0.0) - radius;
}

half2 gradSdRoundedRect(float2 coord, half2 halfSize, half radius) {
    half2 cornerCoord = abs(half2(coord)) - (halfSize - half2(radius));
    half2 maxCorner = max(cornerCoord, half2(0.0));
    half len = length(maxCorner);
    half gradX = step(cornerCoord.y, cornerCoord.x);
    half2 insideGrad = half2(gradX, 1.0 - gradX);
    half2 outsideGrad = maxCorner / max(len, 0.0001);
    half isOutside = step(0.0001, max(cornerCoord.x, cornerCoord.y));
    return half2(sign(coord)) * mix(insideGrad, outsideGrad, isOutside);
}"""

@Language("AGSL")
internal const val RoundedRectRefractionShaderString = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;

$RoundedRectSDF

half circleMap(half x) {
    half clamped = clamp(x, 0.0, 1.0);
    return 1.0 - sqrt(max(0.0, 1.0 - clamped * clamped));
}

half4 main(float2 coord) {
    half2 halfSize = half2(size * 0.5);
    float2 centeredCoord = (coord + offset) - float2(halfSize);
    half4 radii = half4(cornerRadii);
    half radius = radiusAt(centeredCoord, radii);
    
    half sd = sdRoundedRect(centeredCoord, halfSize, radius);
    half rHeight = half(refractionHeight);
    half rAmount = half(refractionAmount);
    half dEffect = half(depthEffect);
    
    half t = clamp(1.0 + sd / max(rHeight, 0.0001), 0.0, 1.0);
    half d = circleMap(t) * rAmount;
    
    half gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    half2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + dEffect * half2(normalize(centeredCoord)));
    
    float2 refractedCoord = coord + float2(d * grad);
    return content.eval(refractedCoord);
}"""

@Language("AGSL")
internal val RoundedRectRefractionWithDispersionShaderString = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;

$RoundedRectSDF

half circleMap(half x) {
    half clamped = clamp(x, 0.0, 1.0);
    return 1.0 - sqrt(max(0.0, 1.0 - clamped * clamped));
}

half4 main(float2 coord) {
    half2 halfSize = half2(size * 0.5);
    float2 centeredCoord = (coord + offset) - float2(halfSize);
    half4 radii = half4(cornerRadii);
    half radius = radiusAt(centeredCoord, radii);
    
    half sd = sdRoundedRect(centeredCoord, halfSize, radius);
    half rHeight = half(refractionHeight);
    half rAmount = half(refractionAmount);
    half dEffect = half(depthEffect);
    half cAberration = half(chromaticAberration);
    
    half t = clamp(1.0 + sd / max(rHeight, 0.0001), 0.0, 1.0);
    half d = circleMap(t) * rAmount;
    
    half gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    half2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + dEffect * half2(normalize(centeredCoord)));
    
    float2 refractedCoord = coord + float2(d * grad);
    half dispersionIntensity = cAberration * ((half(centeredCoord.x) * half(centeredCoord.y)) / max(halfSize.x * halfSize.y, 0.0001));
    float2 dispersedCoord = float2(d * grad * dispersionIntensity);
    
    half4 color = half4(0.0);
    
    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;
    
    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;
    
    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;
    
    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;
    
    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;
    
    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;
    
    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;
    
    return color;
}"""

@Language("AGSL")
internal const val DefaultHighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
layout(color) uniform half4 color;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    half2 halfSize = half2(size * 0.5);
    float2 centeredCoord = coord - float2(halfSize);
    half4 radii = half4(cornerRadii);
    half radius = radiusAt(centeredCoord, radii);
    
    half gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    half2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    half2 normal = half2(cos(angle), sin(angle));
    half d = dot(grad, normal);
    half intensity = pow(abs(d), half(falloff));
    return color * intensity;
}"""

@Language("AGSL")
internal const val AmbientHighlightShaderString = """
uniform float2 size;
uniform float4 cornerRadii;
uniform float angle;
uniform float falloff;

$RoundedRectSDF

half4 main(float2 coord) {
    half2 halfSize = half2(size * 0.5);
    float2 centeredCoord = coord - float2(halfSize);
    half4 radii = half4(cornerRadii);
    half radius = radiusAt(centeredCoord, radii);
    
    half gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    half2 grad = gradSdRoundedRect(centeredCoord, halfSize, gradRadius);
    half2 normal = half2(cos(angle), sin(angle));
    half d = dot(grad, normal);
    half intensity = pow(abs(d), half(falloff));
    half t = step(0.0, d);
    return half4(t, t, t, 1.0) * intensity;
}"""

