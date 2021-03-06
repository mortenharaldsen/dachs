package com.ethlo.dachs.jpa;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import javax.persistence.Entity;
import javax.persistence.PersistenceUnitUtil;

import org.springframework.util.ReflectionUtils;

import com.ethlo.dachs.PropertyChange;

public class EntityUtil
{
    private final PersistenceUnitUtil persistenceUnitUtil;
    
    public EntityUtil(PersistenceUnitUtil persistenceUnitUtil)
    {
        this.persistenceUnitUtil = persistenceUnitUtil;
    }
    
	public List<PropertyChange<?>> extractEntityProperties(Object target, boolean deleted, Predicate<Object> entityFilter, Predicate<Field> fieldFilter)
	{
	    final List<PropertyChange<?>> propChanges = new ArrayList<PropertyChange<?>>();
	    
	    if (entityFilter == null || entityFilter.test(target))
	    {
    		final Map<String, Field> fieldMap = new HashMap<String, Field>();
    		ReflectionUtils.doWithFields(target.getClass(), new ReflectionUtils.FieldCallback()
    		{
    			public void doWith(Field field)
    			{
    				final String fieldName = field.getName();
    				if (! fieldMap.containsKey(fieldName) && fieldFilter.test(field))
    				{
    					field.setAccessible(true);
    					fieldMap.put(fieldName, field);
    					{
    						extractChangeData(propChanges, target, field, deleted, entityFilter, fieldFilter);
    					}
    				}
    			}
    		});
	    }
		return propChanges;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private <T> void extractChangeData(final List<PropertyChange<?>> propChanges, final Object target, Field field, boolean deleted, Predicate<Object> entityFilter, Predicate<Field> fieldFilter)
	{
	    if (! entityFilter.test(target))
	    {
	        return;
	    }
        
	    if (fieldFilter.test(field))
	    {
    		final String fieldName = field.getName();
    		final Object value = getAuditValue(ReflectionUtils.getField(field, target));
    		propChanges.add(new PropertyChange(fieldName, field.getType(), deleted ? value : null, deleted ? null : value));
	    }
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Object getAuditValue(Object value)
    {
	    if (value instanceof Collection)
        {
            final Collection tmp = new LinkedList<>();
            for (Object v : (Collection) value)
            {
                tmp.add(v);
            }
            return tmp;
        }
        else if (value instanceof Map)
        {
            final Map tmp = new LinkedHashMap<>();
            Set<Entry> set = ((Map) value).entrySet();
            for (Entry e : set)
            {
                tmp.put(e.getKey(), (persistenceUnitUtil.getIdentifier(e.getValue())));
            }
            return tmp;
        }
	    return value;
    }

    public boolean isEntity(Object value)
    {
	    if (value != null)
	    {
	        return value.getClass().getAnnotation(Entity.class) != null;
	    }
	    return false;
    }

    public void extractSingle(String attrName, Class<?> attrType, Object oldValue, Object newValue, List<PropertyChange<?>> propChanges)
	{
		if (! Objects.equals(oldValue, newValue))
		{
			@SuppressWarnings({ "rawtypes", "unchecked" })
            final PropertyChange<?> propChange = new PropertyChange(attrName, attrType, oldValue, newValue);
			propChanges.add(propChange);
		}
	}
}
