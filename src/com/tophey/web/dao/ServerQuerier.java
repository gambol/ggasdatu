/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.tophey.web.dao;

import com.tophey.dao.ConnectionFactory;
import com.tophey.common.PageResult;
import com.tophey.model.ServerInfo;
import com.tophey.common.ServerInfoDetail;
import com.tophey.model.ServerSysInfo;
import com.tophey.dao.DBTool;
import java.awt.image.DataBufferByte;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.tophey.utils.JDBCUtils;

/**
 * 
 * @author xiang.fu
 */
public class ServerQuerier {

	private static final String QUERY_SERVER_PAGE_COUNT_BY_CATEGORY = "select count(*) from "
			+ DBTool.getTableName(ServerSysInfo.class) + " sysinfo left join " + DBTool.getTableName(ServerInfo.class)
			+ " info on sysinfo.id=info.id " + "where info.category_id=?";
	private static final String QUERY_SERVER_PAGE_BY_CATEGORY = "select * from "
			+ DBTool.getTableName(ServerSysInfo.class) + " sysinfo left join " + DBTool.getTableName(ServerInfo.class)
			+ " info on sysinfo.id=info.id " + "where info.category_id=? order by sysinfo.score desc limit ?,?";
	private static final String QUERY_SERVER_PAGE_COUNT_BY_CATEGORY_KEYWORD = "select count(*) from "
			+ DBTool.getTableName(ServerInfo.class) + " info left join " + DBTool.getTableName(ServerSysInfo.class)
			+ " sysinfo on sysinfo.id=info.id " + "where info.category_id=? and info.name like ?";
	private static final String QUERY_SERVER_PAGE_BY_CATEGORY_KEYWORD = "select * from "
			+ DBTool.getTableName(ServerInfo.class) + " info left join " + DBTool.getTableName(ServerSysInfo.class)
			+ " sysinfo on sysinfo.id=info.id "
			+ "where info.category_id=? and info.name like ? order by sysinfo.score desc limit ?,?";

	private static final int NUM_PER_PAGE = 20;
	
	// 根据类别，取得私服的数目
	public int getServerCountByCategory(String categoryId) {
		return getCountByCIdKey(categoryId, null);
	}

	// 根据类别 取得私服详细信息
	// 支持分页
	public List<ServerInfoDetail> getSIDByCategoryId(String categoryId, int start, int size) {
		return getSIDByCategoryIdKey(categoryId, start, size, null);
	}

	// 根据类别，取得私服的数目
	public int getCountByCIdKey(String categoryId, String keyword) {
		Connection conn = null;
		PreparedStatement ps = null;
	
		if (keyword == null) {
			keyword = "";
		}
		keyword = "%" + keyword + "%";

		ResultSet rs = null;
			
		try {
			conn = ConnectionFactory.getInstance().getConnection();
			ps = conn.prepareStatement(QUERY_SERVER_PAGE_COUNT_BY_CATEGORY_KEYWORD);
			ps.setString(1, categoryId);
			ps.setString(2, keyword);
			rs = ps.executeQuery();
			if (rs.next()) {
				return rs.getInt(1); 
			}
		} catch (SQLException ex) {
			ex.printStackTrace();
			Logger.getLogger(ServerQuerier.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			DBTool.closeAll(conn, ps, rs);
		}
		return 0;
	}

	// 根据类别 和 标题关键字取得私服详细信息
	// 支持分页
	public List<ServerInfoDetail> getSIDByCategoryIdKey(String categoryId, int start, int size,
			String keyword) {
		Connection conn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String newKey = keyword;
		if (newKey == null) {
			newKey = "";
		}
		newKey = "%" + keyword + "%";

		try {
			conn = ConnectionFactory.getInstance().getConnection();
			ps = conn.prepareStatement(QUERY_SERVER_PAGE_BY_CATEGORY_KEYWORD);
			ps.setString(1, categoryId);
			ps.setString(2, newKey);
			ps.setInt(3, start);
			ps.setInt(4, size);
			System.out.println(ps.toString());
			rs = ps.executeQuery();

			List<ServerInfoDetail> retLis = new ArrayList<ServerInfoDetail>();
			while (rs.next()) {
				ServerInfo si = (ServerInfo) DBTool.getObjectFromRS(rs, ServerInfo.class);
				ServerSysInfo ssi = (ServerSysInfo) DBTool.getObjectFromRS(rs, ServerSysInfo.class);
				retLis.add(new ServerInfoDetail(si, ssi));
			}
			return retLis;
		} catch (SQLException ex) {
			ex.printStackTrace();
			Logger.getLogger(ServerQuerier.class.getName()).log(Level.SEVERE, null, ex);
		} catch (Exception ex) {
			ex.printStackTrace();
			Logger.getLogger(ServerQuerier.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			DBTool.closeAll(conn, ps, rs);
		}
		return null;
	}

	public ServerInfo getServerInfoById(int id) {
		StringBuffer sb = new StringBuffer();
		sb.append("select * from ").append(DBTool.getTableName(ServerInfo.class)).append(" where id = ?");

		return DBTool.queryEntity(ServerInfo.class, sb.toString(), id);
	}

	public ServerSysInfo getServerSysInfoById(int id) {
		String sql = "select * from " + DBTool.getTableName(ServerSysInfo.class) + " where id = ?";
		return DBTool.queryEntity(ServerSysInfo.class, sql, id);
	}

	public PageResult<ServerInfo> getServerInfoByCategoryId(int categoryId) {
		LinkedHashMap<String, Boolean> orderBy = new LinkedHashMap<String, Boolean>();
		orderBy.put("id", false);
		@SuppressWarnings("unchecked")
		PageResult<ServerInfo> pr = JDBCUtils.getPageData(ServerInfo.class, NUM_PER_PAGE,
				1, orderBy, " category_id = ?",	new Object[] { String.valueOf(categoryId) });
		return pr;
	}

	/**
	 * 根據用戶id 選擇 serverInfo 按照创建时间逆序 默认只展示正常(包括隐藏的)状态的server
	 * 
	 */
	public PageResult<ServerInfo> getServerInfoByUserId(int userId, int currentPage) {
		return getServerInfoByUserId(userId, currentPage, false);
	}

	/**
	 * 根據用戶id 選擇 serverInfo 按照创建时间逆序 如果showAll为true,则展示包括隐藏,删除等状态的sql
	 * 
	 */
	public PageResult<ServerInfo> getServerInfoByUserId(int userId, int currentPage, boolean showAll) {
		LinkedHashMap<String, Boolean> orderBy = new LinkedHashMap<String, Boolean>();
		orderBy.put("create_date", false);

		String statusSql = "status in ('online', 'hidden') and ";
		if (showAll) {
			statusSql = "";
		}

		@SuppressWarnings("unchecked")
		PageResult<ServerInfo> pr = JDBCUtils.getPageData(ServerInfo.class, 
				NUM_PER_PAGE, currentPage, orderBy, statusSql	+ " user_id = ?",
				new Object[] { String.valueOf(userId) });
		return pr;
	}

	public static void main(String[] args) {
		// ServerInfo si = new ServerQuerier().getServerInfoById(4);
		// System.out.println(si.getName());

		PageResult pr = new ServerQuerier().getServerInfoByCategoryId(1);
		System.out.println(pr.getTotalCount());
		System.out.println(pr.getPageList().size());

	}
}
