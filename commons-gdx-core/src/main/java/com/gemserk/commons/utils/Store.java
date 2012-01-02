package com.gemserk.commons.utils;

import java.util.ArrayList;

public abstract class Store<T> {

	protected ArrayList<T> created = new ArrayList<T>();
	protected ArrayList<T> free = new ArrayList<T>();

	public T get() {
		if (free.isEmpty())
			return newObject();
		else
			return reuseObject();
	}

	protected T reuseObject() {
		T t = free.get(0);
		free.remove(0);
		created.add(t);
		return t;
	}

	protected T newObject() {
		T t = createObject();
		created.add(t);
		return t;
	}

	protected abstract T createObject();

	public void free(T t) {
		// entities keep being in the world with this.
		free.add(t);
		created.remove(t);
	}

	public int size() {
		return created.size();
	}

	public T get(int index) {
		return created.get(index);
	}
	
	public void preCreate(int count) {
		for (int i = 0; i < count; i++)
			free(createObject());
	}

}