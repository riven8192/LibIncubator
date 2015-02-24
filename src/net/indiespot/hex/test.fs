#version 430

layout(location = 0) uniform vec2 invViewport;
layout(location = 0) out vec4 fragColour;


const float infinity = 10000.0;
const float infinity2 = infinity - 1;




struct ray {
	vec3 origin;
	vec3 dir;
};

struct sphere {
	vec3 color;
	float reflectance;
	
	vec3 origin;
	float radius;
};




float ray_sphere(ray r, sphere s) {
	vec3 v = s.origin - r.origin;
	float b = dot(v, r.dir);
	float disc = b*b - dot(v,v) + s.radius*s.radius;
	if (disc < 0.0) return infinity;
	float d = sqrt(disc);
	float t2 = b + d;
	if (t2 < 0.0) return infinity;
	float t1 = b - d;
	return (t1 > 0.0 ? t1 : t2);
}

vec3 surface_normal(ray r, vec3 sorigin, float dist) {
	return normalize(((dist * r.dir) - sorigin) + r.origin);
}

void reflect_ray(inout ray r, vec3 surfaceNormal) {
    r.dir -= 2.0 * dot(surfaceNormal, r.dir) * surfaceNormal;
}


sphere spheres[4] = sphere[4](
	sphere(vec3(0.8, 0.2, 0.2), 0.25, vec3(0.0, 0.0, 3.0), 0.50),
	sphere(vec3(0.8, 0.2, 0.8), 0.25, vec3(0.5, 0.0,  2.0), 0.125),
	sphere(vec3(0.8, 0.8, 0.8), 0.25, vec3(-0.4, 0.5, 2.5), 0.25),
	sphere(vec3(0.7, 0.7, 0.8), 0.25, vec3(-0.3, -0.75, 1.0), 0.5)
);

const vec3 lights[16] = vec3[16](
	normalize(vec3(-0.11, -0.11, -1.0)),
	normalize(vec3(-0.10, -0.11, -1.0)),
	normalize(vec3(-0.09, -0.11, -1.0)),
	normalize(vec3(-0.08, -0.11, -1.0)),
	
	normalize(vec3(-0.11, -0.10, -1.0)),
	normalize(vec3(-0.10, -0.10, -1.0)),
	normalize(vec3(-0.09, -0.10, -1.0)),
	normalize(vec3(-0.08, -0.10, -1.0)),
	
	normalize(vec3(-0.11, -0.09, -1.0)),
	normalize(vec3(-0.10, -0.09, -1.0)),
	normalize(vec3(-0.09, -0.09, -1.0)),
	normalize(vec3(-0.08, -0.09, -1.0)),
	
	normalize(vec3(-0.11, -0.08, -1.0)),
	normalize(vec3(-0.10, -0.08, -1.0)),
	normalize(vec3(-0.09, -0.08, -1.0)),
	normalize(vec3(-0.08, -0.08, -1.0))
);

const int SPHERE_COUNT = 4;
const int LIGHT_COUNT = 16;
	


void main() {
	vec2 coords = ((gl_FragCoord.xy * invViewport) - vec2(0.5)) * vec2(2.0);	

	
	
	vec3 rgb = vec3(0.0);
	
	ray r = ray(vec3(0.0, 0.0, 0.0), normalize(vec3(coords.x, coords.y, 2.1)));
	
	float ref = 1.0 / LIGHT_COUNT;
	for(int q=0; q<4; q++) {
		sphere hit;
		float dist = infinity;
		for(int i=0; i<SPHERE_COUNT; i++) {
			float d = ray_sphere(r, spheres[i]);
			if(d < dist) {
				hit = spheres[i];
				dist = d;
			}
		}
		
		if(dist >= infinity2) {
			break;
		}
		
		vec3 n = surface_normal(r, hit.origin, dist);
		vec3 p = r.origin + r.dir * dist * 0.99999;
		
		for(int t=0; t<LIGHT_COUNT; t++) {
			vec3 light = lights[t];
			ray trace = ray(p, light);
			float NdotL = max(0.0, dot(n, light));
			for(int i=0; i<SPHERE_COUNT; i++) {
				if(NdotL > 0.0) {
					if(ray_sphere(trace, spheres[i]) < infinity2) {
						NdotL = 0.0;
					}
				}
			}
			rgb += hit.color * NdotL * ref;
		}
		ref *= hit.reflectance;
		
		r.origin = p;
		reflect_ray(r, n);
	}
	
	fragColour = vec4(rgb, 1.0);
}