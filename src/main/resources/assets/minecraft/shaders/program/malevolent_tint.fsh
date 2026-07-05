#version 150

uniform sampler2D DiffuseSampler;
in vec2 texCoord;
out vec4 fragColor;

void main() {
    // Get the original pixel color from the game world
    vec4 color = texture(DiffuseSampler, texCoord);

    // Calculate the luminance (grayscale) of the world
    float luminance = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 grayscale = vec3(luminance);

    // The red tint
    vec3 shrineTint = vec3(1.2, 0.2, 0.2);

    // Mix the original world color with the tinted grayscale
    vec3 finalColor = mix(color.rgb, grayscale * shrineTint, 0.70);

    // 5. Increase the contrast
    finalColor = pow(finalColor, vec3(1.2));

    fragColor = vec4(finalColor, color.a);
}