package com.callke8.system.module;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONArray;

import com.callke8.common.IController;
import com.callke8.system.operator.OperRole;
import com.callke8.system.org.Org;
import com.callke8.system.rolemodule.RoleModule;
import com.callke8.utils.BlankUtils;
import com.callke8.utils.MemoryVariableUtil;
import com.callke8.utils.RenderJson;
import com.callke8.utils.TreeJson;
import com.jfinal.core.Controller;
import com.jfinal.plugin.activerecord.Record;


public class ModuleController extends Controller implements IController {
	
	public void index() {
		render("list.jsp");
	}
	
	/**
	 * 菜单树形
	 * 
	 */
	public void tree() {
		
		List<Record> list = Module.dao.getAllModule();
		List<TreeJson> tjs = new ArrayList<TreeJson>();   //定义一个TreeJson 的 list
		
		//由于菜单的树形是多根节点，所以需要先构建一个虚拟的根节点
		TreeJson tjRoot = new TreeJson();
		tjRoot.setId("-1");
		tjRoot.setText("请选择菜单");
		tjRoot.setPid("root");
		tjs.add(tjRoot);
		
		for(Record r:list) {
			
			TreeJson tj = new TreeJson();
			String uri = r.get("MODULE_URI");
			String desc = r.get("MODULE_DESC");
			
			tj.setId(r.get("MODULE_CODE").toString());
			tj.setText(r.get("MODULE_NAME").toString());
			tj.setPid(r.get("PARENT_CODE").toString());
			if(!BlankUtils.isBlank(uri)) {
				tj.setUri(uri.toString());
			}
			if(!BlankUtils.isBlank(desc)) {
				tj.setDesc(desc);
			}
			
			tjs.add(tj);
		}
		
		List<TreeJson> results = TreeJson.formatTree(tjs);
		JSONArray jsonArray = JSONArray.fromObject(results);
		
		System.out.println("JsonArray----:" + jsonArray.toString());
		
		renderJson(jsonArray.toString());
	}
	
	/**
	 * 用于根据操作员的权限显示不同的菜单
	 */
	public void menu() {
		String currOperId = getPara("currOperId");   //得到当前登录的操作员
		
		//System.out.println("当前登录的操作员：" + currOperId);
		
		//当操作员不为空时，进行菜单控制
		if(!BlankUtils.isBlank(currOperId)) {
		
			List<Record> list  = getModuleByOperator(currOperId);
			
			List<TreeJson> tjs = new ArrayList<TreeJson>();   //定义一个TreeJson 的 list
			
			//由于菜单的树形是多根节点，所以需要先构建一个虚拟的根节点
			TreeJson tjRoot = new TreeJson();
			tjRoot.setId("-1");
			tjRoot.setText("请选择菜单");
			tjRoot.setPid("root");
			tjs.add(tjRoot);
			
			for(Record r:list) {
				
				TreeJson tj = new TreeJson();
				String uri = r.get("MODULE_URI");
				String desc = r.get("MODULE_DESC");
				
				tj.setId(r.get("MODULE_CODE").toString());
				tj.setText(r.get("MODULE_NAME").toString());
				tj.setPid(r.get("PARENT_CODE").toString());
				if(!BlankUtils.isBlank(uri)) {
					tj.setUri(uri.toString());
				}
				if(!BlankUtils.isBlank(desc)) {
					tj.setDesc(desc);
				}
				
				if(r.get("PARENT_CODE").toString().equalsIgnoreCase("-1") && r.get("MODULE_CODE").equals("OUTBOUND_MANAGER")) {   //如果等于负一时
					tj.setState("true");
				}
				
				tjs.add(tj);
			}
			
			List<TreeJson> results = TreeJson.formatTree(tjs);
			JSONArray jsonArray = JSONArray.fromObject(results);
			
			System.out.println("JsonArray----:" + jsonArray.toString());
			
			renderJson(jsonArray.toString());
		} else {   //如果操作员为空时，返回空
			renderJson("");
		}
		
	}
	
	/**
	 * 将当前操作员能操作的菜单转成字符串返回 
	 * 
	 * @param currOperId
	 * 			当前登录的操作员
	 * @return
	 */
	public static String getMenuToString(String currOperId) {
		
		//当操作员不为空时，进行菜单控制
		if(!BlankUtils.isBlank(currOperId)) {
		
			List<Record> list  = getModuleByOperatorForStatic(currOperId);
			
			List<TreeJson> tjs = new ArrayList<TreeJson>();   //定义一个TreeJson 的 list
			
			//由于菜单的树形是多根节点，所以需要先构建一个虚拟的根节点
			TreeJson tjRoot = new TreeJson();
			tjRoot.setId("-1");
			tjRoot.setText("请选择菜单");
			tjRoot.setPid("root");
			tjs.add(tjRoot);
			
			for(Record r:list) {
				
				TreeJson tj = new TreeJson();
				String uri = r.get("MODULE_URI");
				String desc = r.get("MODULE_DESC");
				
				tj.setId(r.get("MODULE_CODE").toString());
				tj.setText(r.get("MODULE_NAME").toString());
				tj.setPid(r.get("PARENT_CODE").toString());
				if(!BlankUtils.isBlank(uri)) {
					tj.setUri(uri.toString());
				}
				if(!BlankUtils.isBlank(desc)) {
					tj.setDesc(desc);
				}
				
				if(r.get("PARENT_CODE").toString().equalsIgnoreCase("-1") && r.get("MODULE_CODE").equals("OUTBOUND_MANAGER")) {   //如果等于负一时
					tj.setState("true");
				}
				
				tjs.add(tj);
			}
			
			List<TreeJson> results = TreeJson.formatTree(tjs);
			JSONArray jsonArray = JSONArray.fromObject(results);
			
			return jsonArray.toString();
		} else {   //如果操作员为空时，返回空
			return "";
		}
		
	}
	
	/**
	 * 根据操作员ID,返回菜单列表
	 * 方法是：（1）根据操作员ID，取出对应的角色编码
	 *         (2) 根据角色编码，取出角色编码对应的菜单
	 *         （3）再根据取出的菜单编码，进行删减
	 * @param operId
	 * @return
	 */
	public List<Record> getModuleByOperator(String operId) {
		
		List<Record> moduleList = new ArrayList<Record>();
		
		//取出操作员对应的角色编码情况，并以List形式返回
		List<String> roleCodes = OperRole.dao.getRoleCodeByOperId(operId);
		
		
		//由于一个操作员可能有多种角色，角色间的权限菜单可能是相同的，所以需要进行去重
		List<String> moduleCodesUniq = new ArrayList<String>();    //当前操作员，所拥有的所有的权限菜单
		
		/**
		 * 遍历角色，并以角色在角色权限表时，查询角色对应的菜单情况
		 */
		for(String rc:roleCodes) {
			
			//根据角色编码，取得菜单编码集
			List<String> moduleCodes = RoleModule.dao.getModuleCodeByRoleCode(rc);
			
			for(String mc:moduleCodes) {
				
				//先判断是否已经包含了该菜单，如果包含了该菜单，则不再加入
				if(!moduleCodesUniq.contains(mc)) {
					moduleCodesUniq.add(mc);
				}
			}
		}
		
		//先将所有的菜单取出，然后根据操作员对应的角色的权限配置，将无权限的菜单删除
		List<Record> mlist = Module.dao.getAllModule();
		
		//对当前操作员所有的权限菜单进行遍历，并删除
		for(Record r:mlist) {      //遍历所有的菜单
			
			String moduleCode = r.get("MODULE_CODE");   //得到菜单编码
			String parentCode = r.get("PARENT_CODE");   //得到父菜单编码
			
			//如果父菜单编码为 -1，表示该菜单是根菜单，理论上应该必须保留的，但是如果是该根菜单下没有子菜单时，就表示需要删除
			if(parentCode.equalsIgnoreCase("-1")) {    
				for(String mc:moduleCodesUniq) {    //再遍历当前操作员所有的权限菜单
					Record module = Module.dao.getModuleByModuleCode(mc);    //根据菜单编码取出菜单对象
					
					if(module.get("PARENT_CODE").equals(moduleCode)) {    //只要有一个菜单对象的父编码与根菜单的编码相同时，就表示该根节点下有子菜单，就需要保留
						moduleList.add(r);
						break;
					}
				}
			}else {     //否则当前的菜单就为非根菜单，只需要将菜单与其相同的编码相同直接加入就行了
				for(String mc:moduleCodesUniq) {
					if(mc.equals(moduleCode)){    //如果相等时，就直接加入
						moduleList.add(r);
						break;
					}
				}
			}
		}
		
		return moduleList;
	}
	
	/**
	 * 根据操作员ID,返回菜单列表
	 * 方法是：（1）根据操作员ID，取出对应的角色编码
	 *         (2) 根据角色编码，取出角色编码对应的菜单
	 *         （3）再根据取出的菜单编码，进行删减
	 * @param operId
	 * @return
	 */
	public static List<Record> getModuleByOperatorForStatic(String operId) {
		
		List<Record> moduleList = new ArrayList<Record>();
		
		//取出操作员对应的角色编码情况，并以List形式返回
		List<String> roleCodes = OperRole.dao.getRoleCodeByOperId(operId);
		
		
		//由于一个操作员可能有多种角色，角色间的权限菜单可能是相同的，所以需要进行去重
		List<String> moduleCodesUniq = new ArrayList<String>();    //当前操作员，所拥有的所有的权限菜单
		
		/**
		 * 遍历角色，并以角色在角色权限表时，查询角色对应的菜单情况
		 */
		for(String rc:roleCodes) {
			
			//根据角色编码，取得菜单编码集
			List<String> moduleCodes = RoleModule.dao.getModuleCodeByRoleCode(rc);
			
			for(String mc:moduleCodes) {
				
				//先判断是否已经包含了该菜单，如果包含了该菜单，则不再加入
				if(!moduleCodesUniq.contains(mc)) {
					moduleCodesUniq.add(mc);
				}
			}
		}
		
		//先将所有的菜单取出，然后根据操作员对应的角色的权限配置，将无权限的菜单删除
		List<Record> mlist = Module.dao.getAllModule();
		
		//对当前操作员所有的权限菜单进行遍历，并删除
		for(Record r:mlist) {      //遍历所有的菜单
			
			String moduleCode = r.get("MODULE_CODE");   //得到菜单编码
			String parentCode = r.get("PARENT_CODE");   //得到父菜单编码
			
			//如果父菜单编码为 -1，表示该菜单是根菜单，理论上应该必须保留的，但是如果是该根菜单下没有子菜单时，就表示需要删除
			if(parentCode.equalsIgnoreCase("-1")) {    
				for(String mc:moduleCodesUniq) {    //再遍历当前操作员所有的权限菜单
					Record module = Module.dao.getModuleByModuleCode(mc);    //根据菜单编码取出菜单对象
					
					if(module.get("PARENT_CODE").equals(moduleCode)) {    //只要有一个菜单对象的父编码与根菜单的编码相同时，就表示该根节点下有子菜单，就需要保留
						moduleList.add(r);
						break;
					}
				}
			}else {     //否则当前的菜单就为非根菜单，只需要将菜单与其相同的编码相同直接加入就行了
				for(String mc:moduleCodesUniq) {
					if(mc.equals(moduleCode)){    //如果相等时，就直接加入
						moduleList.add(r);
						break;
					}
				}
			}
		}
		
		return moduleList;
	}
	
	public void datagrid() {
		
		String moduleCode = getPara("moduleCode");  //得到菜单编码
		//System.out.println("得到参数代码：" + moduleCode);
		
		List<Record> list = Module.dao.getModuleByParentCode(moduleCode);
		
		Map m = new HashMap();
		m.put("total", list.size());
		m.put("rows", list);
		
		renderJson(m);
	}
	
	public void delete() {
		
		String moduleCode = getPara("moduleCode");    //得到菜单编码
		
		//System.out.println("得到参数代码:" + moduleCode);
		
		//先判断当前 moduleCode 下，是否有其他的子菜单，如果有子组织，则禁止删除
		List<Record> list = Module.dao.getModuleByParentCode(moduleCode);
		if(list.size()>0) {   //即是如果有子组织，则禁止删除
			render(RenderJson.warn("删除失败，该菜单下还有子菜单，不允许删除!"));
			return;
		}
		
		boolean b = Module.dao.deleteByModuleCode(moduleCode);
		
		if(b) {
			
			
			//删除成功时，还需要将角色菜单授权的内容删除
			RoleModule.dao.deleteByModuleCode(moduleCode);
			
			//删除成功时，重新加载菜单数据到内存中
			MemoryVariableUtil.moduleMap = Module.dao.loadModuleInfo();
			
			render(RenderJson.success("删除记录成功!"));
		}else {
			render(RenderJson.success("删除记录失败!"));
		}
		
	}
	
	public void update() {
		
		Module module = getModel(Module.class,"module");
		
		boolean b = Module.dao.update(module);
		
		if(b) {
			//编辑成功时，重新加载菜单数据到内存中
			MemoryVariableUtil.moduleMap = Module.dao.loadModuleInfo();
			
			render(RenderJson.success("编辑菜单成功!"));
		}else {
			render(RenderJson.error("编辑菜单失败!"));
		}
		
	}
	
	public void add() {
		
		Module module = getModel(Module.class,"module");
		
		boolean b = Module.dao.add(module);
		
		if(b) {
			//保存成功时，重新加载菜单数据到内存中
			MemoryVariableUtil.moduleMap = Module.dao.loadModuleInfo();
			
			render(RenderJson.success("添加菜单成功!"));
		}else {
			render(RenderJson.error("添加菜单失败!"));
		}
	}
	
}
