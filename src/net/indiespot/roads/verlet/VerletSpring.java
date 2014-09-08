package net.indiespot.roads.verlet;

public class VerletSpring {
	public static final int ENFORCE_FIXED_LENGTH = 0;
	public static final int ENFORCE_MIN_LENGTH = 1;
	public static final int ENFORCE_MAX_LENGTH = 2;

	//

	public final VerletParticle a, b;

	public float len, stf;
	public int how;

	public VerletSpring(VerletParticle a, VerletParticle b) {
		this.a = a;
		this.b = b;
	}

	//

	public final float setCurrentDistanceAsLength() {
		float dx = b.now.x - a.now.x;
		float dy = b.now.y - a.now.y;
		float dz = b.now.z - a.now.z;
		float d2 = dx * dx + dy * dy + dz * dz;
		if(d2 == 0.0f)
			throw new IllegalStateException();

		float d = (float) Math.sqrt(d2);

		this.len = d;

		return d;
	}

	public final float tick() {
		float ax = a.now.x;
		float ay = a.now.y;
		float az = a.now.z;

		float bx = b.now.x;
		float by = b.now.y;
		float bz = b.now.z;

		float dx = ax - bx;
		float dy = ay - by;
		float dz = az - bz;
		float dist2 = dx * dx + dy * dy + dz * dz;
		if(dist2 == 0.0f)
			throw new IllegalStateException(this + " exploded");
		float dist = (float) Math.sqrt(dist2);

		if(how == ENFORCE_MIN_LENGTH) {
			if(dist > len)
				return 0.0f;
		}
		else if(how == ENFORCE_MAX_LENGTH) {
			if(dist < len)
				return 0.0f;
		}

		float tension = (this.len - dist) / dist;
		float force = tension * this.stf;

		float aw = a.invWeight;
		float bw = b.invWeight;

		float f1 = force * aw / (aw + bw);
		float f2 = force * bw / (aw + bw);

		a.now.x = ax + dx * f1;
		a.now.y = ay + dy * f1;
		a.now.z = az + dz * f1;

		b.now.x = bx - dx * f2;
		b.now.y = by - dy * f2;
		b.now.z = bz - dz * f2;

		return tension;
	}

	@Override
	public String toString() {
		return "Spring[" + this.a + ", " + this.b + ", len=" + this.len + "]";
	}
}