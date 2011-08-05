package com.gemserk.commons.artemis.systems;

import static org.junit.Assert.assertThat;

import org.hamcrest.core.IsEqual;
import org.junit.Test;

import com.artemis.Component;
import com.artemis.Entity;
import com.artemis.World;
import com.badlogic.gdx.math.Vector2;
import com.gemserk.commons.artemis.Script;
import com.gemserk.commons.artemis.ScriptJavaImpl;
import com.gemserk.commons.artemis.components.ScriptComponent;
import com.gemserk.commons.artemis.components.SpatialComponent;
import com.gemserk.commons.gdx.games.Spatial;
import com.gemserk.commons.gdx.games.SpatialImpl;
import com.gemserk.componentsengine.utils.Container;
import com.gemserk.componentsengine.utils.Parameters;
import com.gemserk.componentsengine.utils.ParametersWrapper;

public class EntityTemplateTest {

	class HealthComponent extends Component {

		Container health;

		public Container getHealth() {
			return health;
		}

		public HealthComponent(Container health) {
			this.health = health;
		}

	}

	class DamageComponent extends Component {

		float damage;

		public float getDamage() {
			return damage;
		}

		public DamageComponent(float damage) {
			this.damage = damage;
		}

	}

	class WeaponComponent extends Component {

		float damage;

		EntityTemplate bulletTemplate;

		public EntityTemplate getBulletTemplate() {
			return bulletTemplate;
		}

		public float getDamage() {
			return damage;
		}

		public WeaponComponent(float damage, EntityTemplate bulletTemplate) {
			this.damage = damage;
			this.bulletTemplate = bulletTemplate;
		}

	}

	interface EntityTemplate {

		Parameters getDefaultParameters();

		void apply(Entity entity);

		void apply(Entity entity, Parameters parameters);

	}

	class ShipEntityTemplate implements EntityTemplate {

		Parameters defaultParameters = new ParametersWrapper();

		public ShipEntityTemplate() {
			defaultParameters.put("health", new Container(100f, 100f));
		}

		@Override
		public void apply(Entity entity) {
			apply(entity, defaultParameters);
		}

		@Override
		public Parameters getDefaultParameters() {
			return defaultParameters;
		}

		@Override
		public void apply(Entity entity, Parameters parameters) {
			Float x = parameters.get("x", 0f);
			Float y = parameters.get("y", 0f);

			Container health = parameters.get("health");

			entity.addComponent(new SpatialComponent(new SpatialImpl(x, y, 1, 1, 0f)));
			entity.addComponent(new HealthComponent(new Container(health.getCurrent(), health.getTotal())));
		}

	}

	class WeaponEntityTemplate implements EntityTemplate {

		Parameters defaultParameters = new ParametersWrapper();

		public WeaponEntityTemplate() {
			defaultParameters.put("damage", new Float(5f));
			defaultParameters.put("script", new ScriptJavaImpl());
			defaultParameters.put("position", new Vector2());
		}

		@Override
		public void apply(Entity entity) {
			apply(entity, defaultParameters);
		}

		@Override
		public Parameters getDefaultParameters() {
			return defaultParameters;
		}

		@Override
		public void apply(Entity entity, Parameters parameters) {
			Float damage = parameters.get("damage");
			EntityTemplate bulletTemplate = parameters.get("bulletTemplate");
			Script script = parameters.get("script");
			Vector2 position = parameters.get("position");

			entity.addComponent(new WeaponComponent(damage, bulletTemplate));
			entity.addComponent(new SpatialComponent(new SpatialImpl(position.x, position.y, 1f, 1f, 0f)));
			entity.addComponent(new ScriptComponent(script));
		}

	}

	class BulletEntityTemplate implements EntityTemplate {

		Parameters defaultParameters = new ParametersWrapper();

		public BulletEntityTemplate() {
			defaultParameters.put("damage", new Float(5f));
			defaultParameters.put("position", new Vector2());
		}

		@Override
		public void apply(Entity entity) {
			apply(entity, defaultParameters);
		}

		@Override
		public Parameters getDefaultParameters() {
			return defaultParameters;
		}

		@Override
		public void apply(Entity entity, Parameters parameters) {
			Float damage = parameters.get("damage");
			Vector2 position = parameters.get("position");
			entity.addComponent(new DamageComponent(damage));
			entity.addComponent(new SpatialComponent(new SpatialImpl(position.x, position.y, 1f, 1f, 0f)));
		}

	}

	interface EntityFactory {

		Entity instantiate(EntityTemplate template);

		Entity instantiate(EntityTemplate template, Parameters parameters);

	}

	class EntityFactoryImpl implements EntityFactory {

		ParametersWithFallBack parametersWithFallBack;
		World world;

		public EntityFactoryImpl(World world) {
			this.world = world;
			parametersWithFallBack = new ParametersWithFallBack();
		}

		@Override
		public Entity instantiate(EntityTemplate template) {
			return internalInstantiate(template, template.getDefaultParameters());
		}

		@Override
		public Entity instantiate(EntityTemplate template, Parameters parameters) {
			parametersWithFallBack.setFallBackParameters(template.getDefaultParameters());
			parametersWithFallBack.setParameters(parameters);
			return internalInstantiate(template, parametersWithFallBack);
		}

		private Entity internalInstantiate(EntityTemplate template, Parameters parameters) {
			Entity entity = world.createEntity();
			template.apply(entity, parameters);
			entity.refresh();
			return entity;
		}

	}

	class ParametersWithFallBack implements Parameters {

		Parameters parameters;
		Parameters fallBackParameters;

		public void setParameters(Parameters parameters) {
			this.parameters = parameters;
		}

		public void setFallBackParameters(Parameters fallBackParameters) {
			this.fallBackParameters = fallBackParameters;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> T get(String id) {
			Object o = parameters.get(id);
			if (o == null)
				return fallBackParameters.get(id);
			return (T) o;
		}

		@Override
		public <T> T get(String id, T defaultValue) {
			return parameters.get(id, defaultValue);
		}

		@Override
		public void put(String id, Object value) {
			parameters.put(id, value);
		}

	}

	@Test
	public void test() {

		EntityTemplate customShipTemplate = new ShipEntityTemplate() {
			{
				defaultParameters.put("x", 100f);
				defaultParameters.put("y", 200f);
				defaultParameters.put("health", new Container(53f, 250f));
			}
		};

		EntityFactory entityFactory = new EntityFactoryImpl(new World());

		Entity entity = entityFactory.instantiate(customShipTemplate);

		SpatialComponent spatialComponent = entity.getComponent(SpatialComponent.class);
		Spatial spatial = spatialComponent.getSpatial();

		assertThat(spatial.getX(), IsEqual.equalTo(100f));
		assertThat(spatial.getY(), IsEqual.equalTo(200f));

		HealthComponent healthComponent = entity.getComponent(HealthComponent.class);
		Container health = healthComponent.getHealth();

		assertThat(health.getCurrent(), IsEqual.equalTo(53f));
		assertThat(health.getTotal(), IsEqual.equalTo(250f));

	}

	@Test
	public void testModifyPositionByHand() {
		// default parameters through a custom template, could be created when the level starts with custom level information
		EntityTemplate customShipTemplate = new ShipEntityTemplate() {
			{
				defaultParameters.put("health", new Container(250f, 250f));
			}
		};

		EntityFactory entityFactory = new EntityFactoryImpl(new World());

		Entity entity = entityFactory.instantiate(customShipTemplate);

		SpatialComponent spatialComponent = entity.getComponent(SpatialComponent.class);
		Spatial spatial = spatialComponent.getSpatial();

		spatial.setPosition(100f, 100f);
	}

	@Test
	public void weaponInstantiateBullet() {
		EntityTemplate bulletTemplate = new BulletEntityTemplate();

		// modifies the template forever, if we are using the same template in different weapons or something similar, then they will be modified as well.
		bulletTemplate.getDefaultParameters().put("damage", new Float(200f));

		EntityFactory entityFactory = new EntityFactoryImpl(new World());
		Entity bullet = entityFactory.instantiate(bulletTemplate);

		DamageComponent damageComponent = bullet.getComponent(DamageComponent.class);
		assertThat(damageComponent.getDamage(), IsEqual.equalTo(200f));
	}

	@Test
	public void weaponInstantiateBulletWithWeaponParameters() {
		// get the component from the weapon (the current entity)
		WeaponComponent weaponComponent = new WeaponComponent(560f, null);

		// custom template parameters of the weapon to be used when building a new bullet
		// this parameters are stored in each weapon instance.
		ParametersWrapper weaponBulletParameters = new ParametersWrapper();
		weaponBulletParameters.put("damage", weaponComponent.getDamage());
		weaponBulletParameters.put("position", new Vector2());

		// the interesting part here is that I could change it dynamically without changing the template or the instantiation call

		EntityTemplate bulletTemplate = new BulletEntityTemplate();

		EntityFactory entityFactory = new EntityFactoryImpl(new World());
		Entity bullet = entityFactory.instantiate(bulletTemplate, weaponBulletParameters);

		DamageComponent damageComponent = bullet.getComponent(DamageComponent.class);
		assertThat(damageComponent.getDamage(), IsEqual.equalTo(560f));
	}

	@Test
	public void shouldUseDefaultParametersIfParameterMissingFromCustomParameters() {
		ParametersWrapper weaponBulletParameters = new ParametersWrapper();
		// damage and position parameters are missing, but they are on default parameters

		EntityTemplate bulletTemplate = new BulletEntityTemplate();

		EntityFactory entityFactory = new EntityFactoryImpl(new World());
		Entity bullet = entityFactory.instantiate(bulletTemplate, weaponBulletParameters);

		DamageComponent damageComponent = bullet.getComponent(DamageComponent.class);
		assertThat(damageComponent.getDamage(), IsEqual.equalTo(5f));
	}

	@Test
	public void weaponInstantiateBulletWithWeaponParametersWithStyle() {
		World world = new World();

		final EntityFactory entityFactory = new EntityFactoryImpl(world);

		EntityTemplate weaponTemplate = new WeaponEntityTemplate();
		EntityTemplate bulletTemplate = new BulletEntityTemplate();

		ParametersWrapper weaponParameters = new ParametersWrapper();
		weaponParameters.put("damage", 30f);
		weaponParameters.put("bulletTemplate", bulletTemplate);
		weaponParameters.put("position", new Vector2(750f, 125f));
		weaponParameters.put("script", new ScriptJavaImpl() {

			ParametersWrapper bulletParameters = new ParametersWrapper();

			@Override
			public void update(World world, Entity e) {
				WeaponComponent weaponComponent = e.getComponent(WeaponComponent.class);
				SpatialComponent spatialComponent = e.getComponent(SpatialComponent.class);

				bulletParameters.put("damage", weaponComponent.getDamage());
				bulletParameters.put("position", spatialComponent.getPosition());

				EntityTemplate bulletTemplate = weaponComponent.getBulletTemplate();
				Entity bullet = entityFactory.instantiate(bulletTemplate, bulletParameters);

				DamageComponent damageComponent = bullet.getComponent(DamageComponent.class);
				SpatialComponent bulletSpatialComponent = bullet.getComponent(SpatialComponent.class);

				// part of the test
				assertThat(damageComponent.getDamage(), IsEqual.equalTo(30f));
				assertThat(bulletSpatialComponent.getSpatial().getX(), IsEqual.equalTo(750f));
				assertThat(bulletSpatialComponent.getSpatial().getY(), IsEqual.equalTo(125f));
			}
		});

		Entity weapon = entityFactory.instantiate(weaponTemplate, weaponParameters);

		// ... on script system

		ScriptComponent scriptComponent = weapon.getComponent(ScriptComponent.class);
		scriptComponent.getScript().update(world, weapon);
	}

}