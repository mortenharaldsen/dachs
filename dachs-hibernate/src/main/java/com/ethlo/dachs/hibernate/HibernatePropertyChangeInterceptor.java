package com.ethlo.dachs.hibernate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;
import org.springframework.util.ReflectionUtils;

import com.ethlo.dachs.EntityDataChangeImpl;
import com.ethlo.dachs.InternalEntityListener;
import com.ethlo.dachs.PropertyChange;

public class HibernatePropertyChangeInterceptor extends EmptyInterceptor
{
    private static final long serialVersionUID = 4618098551981894684L;

    private InternalEntityListener listener;
    private Predicate<Object> entityFilter;
    private Predicate<Field> fieldFilter;

    public HibernatePropertyChangeInterceptor(InternalEntityListener listener)
    {
        this.listener = listener;
        this.entityFilter = listener.getEntityFilter();
        this.fieldFilter = listener.getFieldFilter();
    }

    @Override
    public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
    {
        final Collection<PropertyChange<?>> props = getProperties(entity, new Object[state.length], state, propertyNames, types);
        listener.deleted(new EntityDataChangeImpl(id, entity, props));
    }

    @Override
    public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types)
    {
        final Collection<PropertyChange<?>> props = getProperties(entity, state, new Object[state.length], propertyNames, types);
        listener.created(new EntityDataChangeImpl(id, entity, props));
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)
    {
        final Collection<PropertyChange<?>> props = getProperties(entity, currentState, previousState, propertyNames, types);
        listener.updated(new EntityDataChangeImpl(id, entity, props));
        return false;
    }

    @SuppressWarnings(
    { "unchecked", "rawtypes" })
    private Collection<PropertyChange<?>> getProperties(Object entity, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types)
    {
        final Set<PropertyChange<?>> retVal = new HashSet<>();

        if (entityFilter.test(entity))
        {
            for (int i = 0; i < propertyNames.length; i++)
            {
                final String propertyName = propertyNames[i];
                final Field field = ReflectionUtils.findField(entity.getClass(), propertyName);
                if (this.fieldFilter.test(field) && !Objects.equals(previousState[i], currentState[i]))
                {
                    final PropertyChange changed = new PropertyChange(propertyName, types[i].getReturnedClass(), previousState[i], currentState[i]);
                    retVal.add(changed);
                }
            }
        }

        return retVal;
    }

    public void postFlush(Iterator entities)
    {
        listener.postFlush(entities);
    }
}