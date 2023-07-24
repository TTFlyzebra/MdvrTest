precision mediump float;
varying vec2 texPosition;
uniform sampler2D sampler_y;
uniform sampler2D sampler_uv;
uniform float chromeKey;
void main()
{
    float y, u, v;
    y = texture2D(sampler_y, texPosition).r;
    u = texture2D(sampler_uv, texPosition).r - 0.5;
    v = texture2D(sampler_uv, texPosition).a - 0.5;
    vec3 rgb;
    rgb.r = y + 1.403 * v;
    rgb.g = y - 0.344 * u - 0.714 * v;
    rgb.b = y + 1.770 * u;

    vec4 texColor = vec4(rgb, 1);
    gl_FragColor = texColor;
}