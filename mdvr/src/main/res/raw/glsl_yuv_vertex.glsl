attribute vec4 vPosition;
attribute vec2 fPosition;
varying vec2 texPosition;
uniform mat4 vMatrix;
void main() {
    gl_Position = vMatrix * vPosition;
    texPosition = fPosition;
}