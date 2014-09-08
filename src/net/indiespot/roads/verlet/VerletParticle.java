package net.indiespot.roads.verlet;

public class VerletParticle {
	public final Vec3 now, old;
	public float invWeight;

	public VerletParticle() {
		this.old = new Vec3();
		this.now = new Vec3();
		this.invWeight = 1.0f;
	}

	public void setInfiniteWeight() {
		this.invWeight = 0.0f;
	}

	public void setWeight(float weight) {
		this.invWeight = 1.0f / weight;
	}

	//

	public final void setPosition(Vec3 vec) {
		this.setPosition(vec.x, vec.y, vec.z);
	}

	public final void setPosition(float x, float y, float z) {
		float xVel = now.x - old.x;
		float yVel = now.y - old.y;
		float zVel = now.z - old.z;

		now.x = x;
		now.y = y;
		now.z = z;

		old.x = x - xVel;
		old.y = y - yVel;
		old.z = z - zVel;
	}

	public final void getPosition(Vec3 vec) {
		vec.x = now.x;
		vec.y = now.y;
		vec.z = now.z;
	}

	//

	public void translate(Vec3 vec) {
		this.translate(vec.x, vec.y, vec.z);
	}

	public void translate(float x, float y, float z) {
		this.setPosition(now.x + x, now.y + y, now.z + z);
	}

	//

	public final void addForce(Vec3 vec) {
		this.addForce(vec.x, vec.y, vec.z);
	}

	public final void addForce(float x, float y, float z) {
		this.addVelocity(x * invWeight, y * invWeight, z * invWeight);
	}

	//

	public final void setVelocity(Vec3 vec) {
		this.setVelocity(vec.x, vec.y, vec.z);
	}

	public final void setVelocity(float x, float y, float z) {
		old.x = now.x - x;
		old.y = now.y - y;
		old.z = now.z - z;
	}

	public final void addVelocity(float x, float y, float z) {
		old.x = old.x - x;
		old.y = old.y - y;
		old.z = old.z - z;
	}

	public final void getVelocity(Vec3 vec) {
		vec.x = now.x - old.x;
		vec.y = now.y - old.y;
		vec.z = now.z - old.z;
	}

	public final void mulVelocity(float factor) {
		float xNow = now.x;
		float yNow = now.y;
		float zNow = now.z;

		old.x = xNow - (xNow - old.x) * factor;
		old.y = yNow - (yNow - old.y) * factor;
		old.z = zNow - (zNow - old.z) * factor;
	}

	//

	public final void tick() {
		float xOld = old.x;
		float yOld = old.y;
		float zOld = old.z;

		old.x = now.x;
		old.y = now.y;
		old.z = now.z;

		now.x += now.x - xOld;
		now.y += now.y - yOld;
		now.z += now.z - zOld;
	}

	@Override
	public String toString() {
		return "Particle[" + this.now + "]";
	}
}
