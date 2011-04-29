package com.gemserk.commons.svg.inkscape;

import javax.vecmath.Matrix3f;

public interface SvgImage extends SvgElement {

	float getX();

	float getY();

	float getWidth();

	float getHeight();

	Matrix3f getTransform();

}