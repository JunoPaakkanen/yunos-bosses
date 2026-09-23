#version 150

uniform sampler2D InSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    // Original world color
    vec4 color = texture(InSampler, texCoord);

    // Luminance calculation
    float lum = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // Blood-red shadow and stark highlight gradient (Shibuya anime palette)
    vec3 shadowTint = vec3(0.15, 0.02, 0.03);
    vec3 midTint = vec3(0.75, 0.18, 0.18);
    vec3 highlightTint = vec3(1.25, 0.85, 0.85);

    vec3 toned;
    if (lum < 0.5) {
        toned = mix(shadowTint, midTint, lum * 2.0);
    } else {
        toned = mix(midTint, highlightTint, (lum - 0.5) * 2.0);
    }

    // Blend desaturated blood-toned world with original colors (let pure emissives keep a bit of vibrance)
    vec3 finalColor = mix(color.rgb, toned, 0.78);

    // Dynamic contrast enhancement
    finalColor = pow(finalColor, vec3(1.35));

    // Cinematic dark crimson vignette
    vec2 uv = texCoord - 0.5;
    float dist = length(uv);
    float vignette = smoothstep(0.78, 0.25, dist);
    
    // Edges fade into deep menacing dark red/black
    vec3 vignetteColor = vec3(0.06, 0.005, 0.005);
    finalColor = mix(vignetteColor, finalColor, vignette);

    fragColor = vec4(finalColor, color.a);
}
