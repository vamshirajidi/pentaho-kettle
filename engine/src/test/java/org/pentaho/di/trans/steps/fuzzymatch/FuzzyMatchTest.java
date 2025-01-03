/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


package org.pentaho.di.trans.steps.fuzzymatch;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.pentaho.di.core.RowSet;
import org.pentaho.di.core.exception.KettleStepException;
import org.pentaho.di.core.logging.LoggingObjectInterface;
import org.pentaho.di.core.row.RowMeta;
import org.pentaho.di.core.row.RowMetaInterface;
import org.pentaho.di.core.row.ValueMetaInterface;
import org.pentaho.di.core.row.value.ValueMetaString;
import org.pentaho.di.trans.Trans;
import org.pentaho.di.trans.TransMeta;
import org.pentaho.di.trans.step.StepDataInterface;
import org.pentaho.di.trans.step.StepIOMetaInterface;
import org.pentaho.di.trans.step.StepMeta;
import org.pentaho.di.trans.step.errorhandling.StreamInterface;
import org.pentaho.di.trans.steps.mock.StepMockHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * User: Dzmitry Stsiapanau Date: 10/16/13 Time: 6:23 PM
 */
public class FuzzyMatchTest {
  @InjectMocks
  private FuzzyMatchHandler fuzzyMatch;
  private StepMockHelper<FuzzyMatchMeta, FuzzyMatchData> mockHelper;

  private Object[] row = new Object[] { "Catrine" };
  private Object[] rowB = new Object[] { "Catrine".getBytes() };
  private Object[] row2 = new Object[] { "John" };
  private Object[] row2B = new Object[] { "John".getBytes() };
  private Object[] row3 = new Object[] { "Catriny" };
  private Object[] row3B = new Object[] { "Catriny".getBytes() };
  private List<Object[]> rows = new ArrayList<Object[]>();
  private List<Object[]> binaryRows = new ArrayList<Object[]>();
  private List<Object[]> lookupRows = new ArrayList<Object[]>();
  private List<Object[]> binaryLookupRows = new ArrayList<Object[]>();
  {
    rows.add( row );
    binaryRows.add( rowB );
    lookupRows.add( row2 );
    lookupRows.add( row3 );
    binaryLookupRows.add( row2B );
    binaryLookupRows.add( row3B );
  }

  private class FuzzyMatchHandler extends FuzzyMatch {
    private Object[] resultRow = null;
    private RowSet rowset = null;

    public FuzzyMatchHandler( StepMeta stepMeta, StepDataInterface stepDataInterface, int copyNr, TransMeta transMeta,
        Trans trans ) {
      super( stepMeta, stepDataInterface, copyNr, transMeta, trans );
    }

    @Override
    public void putRow( RowMetaInterface rowMeta, Object[] row ) throws KettleStepException {
      resultRow = row;
    }

    /**
     * Find input row set.
     *
     * @param sourceStep
     *          the source step
     * @return the row set
     * @throws org.pentaho.di.core.exception.KettleStepException
     *           the kettle step exception
     */
    @Override
    public RowSet findInputRowSet( String sourceStep ) throws KettleStepException {
      return rowset;
    }
  }

  @Before
  public void setUp() throws Exception {
    mockHelper =
        new StepMockHelper<FuzzyMatchMeta, FuzzyMatchData>( "Fuzzy Match", FuzzyMatchMeta.class, FuzzyMatchData.class );
    when( mockHelper.logChannelInterfaceFactory.create( any(), any( LoggingObjectInterface.class ) ) ).thenReturn(
        mockHelper.logChannelInterface );
    when( mockHelper.trans.isRunning() ).thenReturn( true );
  }

  @After
  public void tearDown() throws Exception {
    mockHelper.cleanUp();
  }

  @SuppressWarnings( "unchecked" )
  @Test
  public void testProcessRow() throws Exception {
    fuzzyMatch =
        new FuzzyMatchHandler( mockHelper.stepMeta, mockHelper.stepDataInterface, 0, mockHelper.transMeta,
            mockHelper.trans );
    fuzzyMatch.init( mockHelper.initStepMetaInterface, mockHelper.initStepDataInterface );
    RowSet mockRows = mockHelper.getMockInputRowSet( rows );
    RowMetaInterface rowMetaInterface = new RowMeta();
    ValueMetaInterface valueMeta = new ValueMetaString( "field1" );
    valueMeta.setStorageMetadata( new ValueMetaString( "field1" ) );
    rowMetaInterface.addValueMeta( valueMeta );
    when( mockRows.getRowMeta() ).thenReturn( rowMetaInterface );
    fuzzyMatch.addRowSetToInputRowSets( mockRows );
    RowSet mockLookupRows = mockHelper.getMockInputRowSet( lookupRows );
    RowMetaInterface lookupRowMetaInterface = new RowMeta();
    ValueMetaInterface valueMeta2 = new ValueMetaString( "field1" );
    valueMeta2.setStorageMetadata( new ValueMetaString( "field1" ) );
    lookupRowMetaInterface.addValueMeta( valueMeta2 );
    when( mockLookupRows.getRowMeta() ).thenReturn( lookupRowMetaInterface );
    fuzzyMatch.addRowSetToInputRowSets( mockLookupRows );

    when( mockHelper.processRowsStepMetaInterface.getAlgorithmType() ).thenReturn( 8 );
    when(mockHelper.processRowsStepMetaInterface.getMainStreamField() ).thenReturn( "field1" );
    mockHelper.processRowsStepDataInterface.look = mock( HashSet.class );
    when( mockHelper.processRowsStepDataInterface.look.iterator() ).thenReturn( lookupRows.iterator() );

    fuzzyMatch.processRow( mockHelper.processRowsStepMetaInterface, mockHelper.processRowsStepDataInterface );
    Assert.assertEquals( row3[0], fuzzyMatch.resultRow[1] );
  }

  @Test
  public void testReadLookupValues() throws Exception {
    FuzzyMatchData data = spy( new FuzzyMatchData() );
    data.indexOfCachedFields = new int[2];
    data.minimalDistance = 0;
    data.maximalDistance = 5;
    FuzzyMatchMeta meta = spy( new FuzzyMatchMeta() );
    meta.setOutputMatchField( "I don't want NPE here!" );
    data.readLookupValues = true;
    fuzzyMatch =
        new FuzzyMatchHandler( mockHelper.stepMeta, mockHelper.stepDataInterface, 0, mockHelper.transMeta,
            mockHelper.trans );

    fuzzyMatch.init( meta, data );
    RowSet lookupRowSet = mockHelper.getMockInputRowSet( binaryLookupRows );
    fuzzyMatch.addRowSetToInputRowSets( mockHelper.getMockInputRowSet( binaryRows ) );
    fuzzyMatch.addRowSetToInputRowSets( lookupRowSet );
    fuzzyMatch.rowset = lookupRowSet;

    RowMetaInterface rowMetaInterface = new RowMeta();
    ValueMetaInterface valueMeta = new ValueMetaString( "field1" );
    valueMeta.setStorageMetadata( new ValueMetaString( "field1" ) );
    valueMeta.setStorageType( ValueMetaInterface.STORAGE_TYPE_BINARY_STRING );
    rowMetaInterface.addValueMeta( valueMeta );
    when( lookupRowSet.getRowMeta() ).thenReturn( rowMetaInterface );
    when( meta.getLookupField() ).thenReturn( "field1" );
    when( meta.getMainStreamField() ).thenReturn( "field1" );
    fuzzyMatch.setInputRowMeta( rowMetaInterface.clone() );

    when( meta.getAlgorithmType() ).thenReturn( 1 );
    StepIOMetaInterface stepIOMetaInterface = mock( StepIOMetaInterface.class );
    when( meta.getStepIOMeta() ).thenReturn( stepIOMetaInterface );
    StreamInterface streamInterface = mock( StreamInterface.class );
    List<StreamInterface> streamInterfaceList = new ArrayList<StreamInterface>();
    streamInterfaceList.add( streamInterface );
    when( streamInterface.getStepMeta() ).thenReturn( mockHelper.stepMeta );

    when( stepIOMetaInterface.getInfoStreams() ).thenReturn( streamInterfaceList );

    fuzzyMatch.processRow( meta, data );
    Assert.assertEquals( rowMetaInterface.getString( row3B, 0 ),
        data.outputRowMeta.getString( fuzzyMatch.resultRow, 1 ) );
  }

  @Test
  public void testLookupValuesWhenMainFieldIsNull() throws Exception {
    FuzzyMatchData data = spy( new FuzzyMatchData() );
    FuzzyMatchMeta meta = spy( new FuzzyMatchMeta() );
    data.readLookupValues = false;
    fuzzyMatch =
            new FuzzyMatchHandler( mockHelper.stepMeta, mockHelper.stepDataInterface, 0, mockHelper.transMeta,
                    mockHelper.trans );
    fuzzyMatch.init( meta, data );
    fuzzyMatch.first = false;
    data.indexOfMainField = 1;
    Object[] inputRow = { "test input", null };
    RowSet lookupRowSet = mockHelper.getMockInputRowSet( new Object[]{ "test lookup" } );
    fuzzyMatch.addRowSetToInputRowSets( mockHelper.getMockInputRowSet( inputRow ) );
    fuzzyMatch.addRowSetToInputRowSets( lookupRowSet );
    fuzzyMatch.rowset = lookupRowSet;

    RowMetaInterface rowMetaInterface = new RowMeta();
    ValueMetaInterface valueMeta = new ValueMetaString( "field1" );
    valueMeta.setStorageMetadata( new ValueMetaString( "field1" ) );
    valueMeta.setStorageType( ValueMetaInterface.TYPE_STRING );
    rowMetaInterface.addValueMeta( valueMeta );
    when( lookupRowSet.getRowMeta() ).thenReturn( rowMetaInterface );
    fuzzyMatch.setInputRowMeta( rowMetaInterface.clone() );
    data.outputRowMeta = rowMetaInterface.clone();

    fuzzyMatch.processRow( meta, data );
    Assert.assertEquals( inputRow[0], fuzzyMatch.resultRow[0] );
    Assert.assertNull( fuzzyMatch.resultRow[1] );
    Assert.assertTrue( Arrays.stream( fuzzyMatch.resultRow, 3, fuzzyMatch.resultRow.length ).allMatch( val ->  val == null ) );
  }
}
